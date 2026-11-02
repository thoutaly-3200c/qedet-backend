package q.qedet.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import q.qedet.auth.dto.ChangePasswordRequest;
import q.qedet.auth.dto.ForgotPasswordRequest;
import q.qedet.auth.dto.ResetPasswordRequest;
import q.qedet.auth.entity.PasswordResetToken;
import q.qedet.auth.entity.UserAccount;
import q.qedet.auth.repository.PasswordResetTokenRepository;
import q.qedet.auth.repository.UserAccountRepository;
import q.qedet.auth.security.TokenHasher;
import q.qedet.auth.security.TokenService;
import q.qedet.common.rate.RateLimitService;
import q.qedet.email.EmailService;
import q.qedet.common.exception.BadRequestException;
import q.qedet.common.exception.TooManyRequestsException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordManagementServiceTest {

    @Mock private UserAccountRepository users;
    @Mock private PasswordResetTokenRepository resetTokens;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordPolicy passwordPolicy;
    @Mock private TokenService tokenService;
    @Mock private EmailService emailService;
    @Mock private RateLimitService rateLimitService;
    @InjectMocks private PasswordManagementService service;

    private UserAccount user;

    @BeforeEach
    void setUp() {
        user = new UserAccount("user@example.com", "old-hash");
        user.setId(UUID.randomUUID());
    }

    @Test
    void forgotPasswordReturnsSameMessageForExistingAndUnknownEmailsAndStoresOnlyHash() {
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(users.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());
        when(resetTokens.findAllByUserIdAndUsedAtIsNullAndExpiresAtAfter(eq(user.getId()), any()))
                .thenReturn(List.of());

        String existing = service.requestReset(new ForgotPasswordRequest("user@example.com"), "127.0.0.1");
        String unknown = service.requestReset(new ForgotPasswordRequest("unknown@example.com"), "127.0.0.1");

        assertThat(existing).isEqualTo(PasswordManagementService.FORGOT_PASSWORD_MESSAGE);
        assertThat(unknown).isEqualTo(existing);
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(resetTokens).save(tokenCaptor.capture());
        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("user@example.com"), rawTokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo(TokenHasher.sha256(rawTokenCaptor.getValue()));
        assertThat(tokenCaptor.getValue().getTokenHash()).isNotEqualTo(rawTokenCaptor.getValue());
    }

    @Test
    void aNewResetRequestInvalidatesPriorActiveTokens() {
        PasswordResetToken previous = new PasswordResetToken(user, "old-token-hash", Instant.now().plusSeconds(60));
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(resetTokens.findAllByUserIdAndUsedAtIsNullAndExpiresAtAfter(eq(user.getId()), any()))
                .thenReturn(List.of(previous));

        service.requestReset(new ForgotPasswordRequest("user@example.com"), "127.0.0.1");

        assertThat(previous.getUsedAt()).isNotNull();
    }

    @Test
    void rateLimitedForgotPasswordRequestIsRejected() {
        doThrow(new TooManyRequestsException("Too many password reset requests. Please try again later."))
                .when(rateLimitService).requireAllowed(startsWith("password-reset:email:"), anyInt(), any(), anyString());

        assertThatThrownBy(() -> service.requestReset(new ForgotPasswordRequest("user@example.com"), "127.0.0.1"))
                .isInstanceOf(TooManyRequestsException.class);
        verifyNoInteractions(users, emailService);
    }

    @Test
    void resetPasswordAcceptsValidUnusedTokenThenRevokesEverySession() {
        String rawToken = "valid-token";
        PasswordResetToken token = new PasswordResetToken(user, TokenHasher.sha256(rawToken), Instant.now().plusSeconds(60));
        when(resetTokens.findByTokenHash(TokenHasher.sha256(rawToken))).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("new-hash");

        service.resetPassword(new ResetPasswordRequest(rawToken, "NewPassword123!", "NewPassword123!"), "127.0.0.1");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(token.getUsedAt()).isNotNull();
        verify(tokenService).revokeAllForUser(user.getId());
        assertThatThrownBy(() -> service.resetPassword(
                new ResetPasswordRequest(rawToken, "NewPassword123!", "NewPassword123!"), "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(PasswordManagementService.INVALID_RESET_TOKEN_MESSAGE);
    }

    @Test
    void resetPasswordRejectsInvalidExpiredAndUsedTokensWithoutChangingPassword() {
        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("missing", "NewPassword123!", "NewPassword123!"), "ip"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(PasswordManagementService.INVALID_RESET_TOKEN_MESSAGE);

        PasswordResetToken expired = new PasswordResetToken(user, TokenHasher.sha256("expired"), Instant.now().minusSeconds(1));
        when(resetTokens.findByTokenHash(TokenHasher.sha256("expired"))).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("expired", "NewPassword123!", "NewPassword123!"), "ip"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(PasswordManagementService.EXPIRED_RESET_TOKEN_MESSAGE);

        PasswordResetToken used = new PasswordResetToken(user, TokenHasher.sha256("used"), Instant.now().plusSeconds(60));
        used.setUsedAt(Instant.now());
        when(resetTokens.findByTokenHash(TokenHasher.sha256("used"))).thenReturn(Optional.of(used));
        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("used", "NewPassword123!", "NewPassword123!"), "ip"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(PasswordManagementService.INVALID_RESET_TOKEN_MESSAGE);
        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
    }

    @Test
    void resetPasswordRejectsMismatchedAndPolicyInvalidPasswords() {
        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("token", "NewPassword123!", "different"), "ip"))
                .isInstanceOf(BadRequestException.class);
        doThrow(new BadRequestException("Password policy failure")).when(passwordPolicy).validate("weak");
        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("token", "weak", "weak"), "ip"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Password policy failure");
        verifyNoInteractions(passwordEncoder, tokenService);
    }

    @Test
    void changePasswordRejectsWrongOrUnchangedPasswordAndChangesCorrectPassword() {
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("incorrect", "old-hash")).thenReturn(false);
        assertThatThrownBy(() -> service.changePassword(user.getId(), new ChangePasswordRequest("incorrect", "NewPassword123!", "NewPassword123!")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Current password is incorrect.");

        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
        assertThatThrownBy(() -> service.changePassword(user.getId(), new ChangePasswordRequest("current", "old-password", "old-password")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("New password must be different from the current password.");

        when(passwordEncoder.matches("NewPassword123!", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("new-hash");
        service.changePassword(user.getId(), new ChangePasswordRequest("current", "NewPassword123!", "NewPassword123!"));
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(tokenService).revokeAllForUser(user.getId());
    }
}
