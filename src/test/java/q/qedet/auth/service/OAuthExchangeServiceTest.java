package q.qedet.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import q.qedet.auth.dto.AuthResponse;
import q.qedet.auth.entity.AuthSession;
import q.qedet.auth.entity.OAuthExchangeCode;
import q.qedet.auth.entity.UserAccount;
import q.qedet.auth.repository.OAuthExchangeCodeRepository;
import q.qedet.auth.security.TokenHasher;
import q.qedet.auth.security.TokenService;
import q.qedet.common.exception.BadRequestException;
import q.qedet.common.exception.UnauthorizedException;
import q.qedet.common.model.RegistrationStatus;
import q.qedet.common.model.UserStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthExchangeServiceTest {

    @Mock private OAuthExchangeCodeRepository exchangeCodeRepository;
    @Mock private TokenService tokenService;
    @Mock private AuthService authService;
    @InjectMocks private OAuthExchangeService service;

    @Test
    void createCodePersistsHashedCodeAndTokenPair() {
        UserAccount user = activeUser();
        AuthSession session = new AuthSession(user, null, "access-hash", "refresh-hash",
                Instant.now().plusSeconds(60), Instant.now().plusSeconds(120));
        TokenService.TokenPair tokens = new TokenService.TokenPair("access-token", "refresh-token");

        when(tokenService.authenticate("access-token")).thenReturn(session);
        when(exchangeCodeRepository.save(any(OAuthExchangeCode.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String code = service.createCode(tokens);

        ArgumentCaptor<OAuthExchangeCode> captor = ArgumentCaptor.forClass(OAuthExchangeCode.class);
        verify(exchangeCodeRepository).save(captor.capture());
        OAuthExchangeCode saved = captor.getValue();
        assertThat(code).isNotBlank();
        assertThat(saved.getCodeHash()).isEqualTo(TokenHasher.sha256(code));
        assertThat(saved.getAccessToken()).isEqualTo("access-token");
        assertThat(saved.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(saved.getAuthSession()).isSameAs(session);
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void exchangeReturnsNormalAuthResponseForUserWithoutOrganization() {
        UserAccount user = activeUser();
        user.setRegistrationStatus(RegistrationStatus.PERSONAL_INFORMATION);
        AuthSession session = new AuthSession(user, null, "access-hash", "refresh-hash",
                Instant.now().plusSeconds(60), Instant.now().plusSeconds(120));
        OAuthExchangeCode exchangeCode = validExchange(session, "one-time-code");
        AuthResponse expected = new AuthResponse(
                "access-token", "refresh-token", user.getId(), null, null, null,
                Set.of(), RegistrationStatus.PERSONAL_INFORMATION, null, null, null,
                "user", null, "user@example.com"
        );

        when(exchangeCodeRepository.findByCodeHash(TokenHasher.sha256("one-time-code")))
                .thenReturn(Optional.of(exchangeCode));
        when(exchangeCodeRepository.consumeIfValid(eq(exchangeCode.getCodeHash()), any(), any()))
                .thenReturn(1);
        when(tokenService.authenticate("access-token")).thenReturn(session);
        when(authService.authResponseForSession(eq(session), any(TokenService.TokenPair.class)))
                .thenReturn(expected);

        AuthResponse response = service.exchange("one-time-code");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.organizationId()).isNull();
        assertThat(response.employeeId()).isNull();
        verify(exchangeCodeRepository).clearStoredTokens(exchangeCode.getCodeHash());
    }

    @Test
    void exchangeRejectsMissingCode() {
        assertThatThrownBy(() -> service.exchange("  "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Missing");
    }

    @Test
    void exchangeRejectsInvalidCode() {
        when(exchangeCodeRepository.findByCodeHash(TokenHasher.sha256("random-code")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exchange("random-code"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(ex -> ((UnauthorizedException) ex).getCode())
                .isEqualTo("INVALID_OAUTH_EXCHANGE_CODE");
    }

    @Test
    void exchangeRejectsExpiredCode() {
        UserAccount user = activeUser();
        AuthSession session = new AuthSession(user, null, "access-hash", "refresh-hash",
                Instant.now().plusSeconds(60), Instant.now().plusSeconds(120));
        OAuthExchangeCode exchangeCode = new OAuthExchangeCode(
                session,
                TokenHasher.sha256("expired-code"),
                "access-token",
                "refresh-token",
                Instant.now().minusSeconds(5)
        );
        when(exchangeCodeRepository.findByCodeHash(TokenHasher.sha256("expired-code")))
                .thenReturn(Optional.of(exchangeCode));

        assertThatThrownBy(() -> service.exchange("expired-code"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(ex -> ((UnauthorizedException) ex).getCode())
                .isEqualTo("OAUTH_EXCHANGE_CODE_EXPIRED");
        verify(exchangeCodeRepository, never()).consumeIfValid(any(), any(), any());
    }

    @Test
    void exchangeRejectsAlreadyConsumedCode() {
        UserAccount user = activeUser();
        AuthSession session = new AuthSession(user, null, "access-hash", "refresh-hash",
                Instant.now().plusSeconds(60), Instant.now().plusSeconds(120));
        OAuthExchangeCode exchangeCode = validExchange(session, "used-code");
        exchangeCode.setConsumedAt(Instant.now().minusSeconds(1));
        when(exchangeCodeRepository.findByCodeHash(TokenHasher.sha256("used-code")))
                .thenReturn(Optional.of(exchangeCode));

        assertThatThrownBy(() -> service.exchange("used-code"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(ex -> ((UnauthorizedException) ex).getCode())
                .isEqualTo("OAUTH_EXCHANGE_CODE_USED");
        verify(exchangeCodeRepository, never()).consumeIfValid(any(), any(), any());
    }

    @Test
    void exchangeRejectsRaceWhereConsumeFails() {
        UserAccount user = activeUser();
        AuthSession session = new AuthSession(user, null, "access-hash", "refresh-hash",
                Instant.now().plusSeconds(60), Instant.now().plusSeconds(120));
        OAuthExchangeCode exchangeCode = validExchange(session, "race-code");
        when(exchangeCodeRepository.findByCodeHash(TokenHasher.sha256("race-code")))
                .thenReturn(Optional.of(exchangeCode));
        when(exchangeCodeRepository.consumeIfValid(eq(exchangeCode.getCodeHash()), any(), any()))
                .thenReturn(0);

        assertThatThrownBy(() -> service.exchange("race-code"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(ex -> ((UnauthorizedException) ex).getCode())
                .isEqualTo("OAUTH_EXCHANGE_CODE_USED");
    }

    private static UserAccount activeUser() {
        UserAccount user = new UserAccount("user@example.com", "hash");
        user.setId(UUID.randomUUID());
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        return user;
    }

    private static OAuthExchangeCode validExchange(AuthSession session, String rawCode) {
        return new OAuthExchangeCode(
                session,
                TokenHasher.sha256(rawCode),
                "access-token",
                "refresh-token",
                Instant.now().plusSeconds(60)
        );
    }
}
