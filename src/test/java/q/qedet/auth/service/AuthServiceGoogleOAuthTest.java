package q.qedet.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import q.qedet.auth.dto.GoogleIdentityClaims;
import q.qedet.auth.entity.UserAccount;
import q.qedet.auth.repository.EmailVerificationTokenRepository;
import q.qedet.auth.repository.UserAccountRepository;
import q.qedet.auth.security.TokenService;
import q.qedet.common.exception.UnauthorizedException;
import q.qedet.common.model.RegistrationStatus;
import q.qedet.common.model.UserStatus;
import q.qedet.common.usage.UsageTracker;
import q.qedet.email.EmailService;
import q.qedet.employee.repository.EmployeeRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceGoogleOAuthTest {

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;
    @Mock private InvitationService invitationService;
    @Mock private UsageTracker usageTracker;
    @Mock private PasswordPolicy passwordPolicy;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private EmailService emailService;
    @InjectMocks private AuthService authService;

    @Test
    void newVerifiedGoogleIdentityCreatesVerifiedPartiallyRegisteredUserAndSession() {
        GoogleIdentityClaims claims = claims("google-subject", "new@example.com");
        when(userAccountRepository.findByGoogleSubject("google-subject")).thenReturn(Optional.empty());
        when(userAccountRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("generated-password-hash");
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(employeeRepository.findAllByUserId(any())).thenReturn(List.of());
        TokenService.TokenPair pair = new TokenService.TokenPair("access", "refresh");
        when(tokenService.createSession(any(UserAccount.class), eq(null), eq(false))).thenReturn(pair);

        assertThat(authService.authenticateGoogleUser(claims)).isSameAs(pair);

        ArgumentCaptor<UserAccount> users = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(users.capture());
        UserAccount created = users.getValue();
        assertThat(created.getEmail()).isEqualTo("new@example.com");
        assertThat(created.getGoogleSubject()).isEqualTo("google-subject");
        assertThat(created.isEmailVerified()).isTrue();
        assertThat(created.getRegistrationStatus()).isEqualTo(RegistrationStatus.PERSONAL_INFORMATION);
        assertThat(created.getRegistrationName()).isEqualTo("New User");
        assertThat(created.getGooglePictureUrl()).isEqualTo("https://example.com/picture.png");
        verify(tokenService).createSession(created, null, false);
    }

    @Test
    void verifiedGoogleEmailLinksExistingPasswordAccountWithoutCreatingAnotherUser() {
        UserAccount existing = activeUser("user@example.com");
        when(userAccountRepository.findByGoogleSubject("new-google-subject")).thenReturn(Optional.empty());
        when(userAccountRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(existing));
        when(employeeRepository.findAllByUserId(existing.getId())).thenReturn(List.of());
        when(tokenService.createSession(existing, null, false)).thenReturn(new TokenService.TokenPair("access", "refresh"));

        authService.authenticateGoogleUser(claims("new-google-subject", "user@example.com"));

        assertThat(existing.getGoogleSubject()).isEqualTo("new-google-subject");
        assertThat(existing.isEmailVerified()).isTrue();
        verify(userAccountRepository).save(existing);
        verify(tokenService).createSession(existing, null, false);
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void subjectLookupUsesExistingAccountAndDoesNotOverwritePersonalInformation() {
        UserAccount existing = activeUser("original@example.com");
        existing.setGoogleSubject("google-subject");
        existing.setEmailVerified(true);
        existing.setRegistrationName("Original Name");
        existing.setGooglePictureUrl("https://example.com/original.png");
        when(userAccountRepository.findByGoogleSubject("google-subject")).thenReturn(Optional.of(existing));
        when(employeeRepository.findAllByUserId(existing.getId())).thenReturn(List.of());
        when(tokenService.createSession(existing, null, false)).thenReturn(new TokenService.TokenPair("access", "refresh"));

        authService.authenticateGoogleUser(claims("google-subject", "changed@example.com"));

        assertThat(existing.getEmail()).isEqualTo("original@example.com");
        assertThat(existing.getRegistrationName()).isEqualTo("Original Name");
        assertThat(existing.getGooglePictureUrl()).isEqualTo("https://example.com/original.png");
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void disabledAccountIsRejectedWithoutLinkingOrCreatingSession() {
        UserAccount disabled = activeUser("disabled@example.com");
        disabled.setStatus(UserStatus.INACTIVE);
        when(userAccountRepository.findByGoogleSubject("google-subject")).thenReturn(Optional.empty());
        when(userAccountRepository.findByEmailIgnoreCase("disabled@example.com")).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> authService.authenticateGoogleUser(claims("google-subject", "disabled@example.com")))
                .isInstanceOf(UnauthorizedException.class);

        assertThat(disabled.getGoogleSubject()).isNull();
        verify(tokenService, never()).createSession(any(), any(), anyBoolean());
    }

    private static GoogleIdentityClaims claims(String subject, String email) {
        return new GoogleIdentityClaims(subject, email, "New User", "New", "User", "https://example.com/picture.png");
    }

    private static UserAccount activeUser(String email) {
        UserAccount user = new UserAccount(email, "password-hash");
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);
        return user;
    }
}
