package q.qedet.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import q.qedet.auth.dto.AuthResponse;
import q.qedet.auth.entity.AuthSession;
import q.qedet.auth.entity.OAuthExchangeCode;
import q.qedet.auth.repository.OAuthExchangeCodeRepository;
import q.qedet.auth.security.TokenHasher;
import q.qedet.auth.security.TokenService;
import q.qedet.common.exception.BadRequestException;
import q.qedet.common.exception.UnauthorizedException;
import q.qedet.common.model.UserStatus;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Issues and consumes short-lived, single-use OAuth exchange codes that bridge
 * the Google OAuth browser redirect and the normal Qedet bearer-token API.
 *
 * <p>Contract:
 * <ul>
 *   <li>Redirect carries only {@code code} (never Qedet or Google tokens).</li>
 *   <li>{@code POST /api/v1/auth/oauth/exchange} with {@code {"code":"..."}} returns
 *       the same {@link AuthResponse} shape as email/password login.</li>
 *   <li>Codes are hashed at rest, TTL-bound, and single-use.</li>
 * </ul>
 */
@Service
public class OAuthExchangeService {

    private static final Logger log = LoggerFactory.getLogger(OAuthExchangeService.class);
    private static final Duration CODE_TTL = Duration.ofMinutes(2);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OAuthExchangeCodeRepository exchangeCodeRepository;
    private final TokenService tokenService;
    private final AuthService authService;

    public OAuthExchangeService(OAuthExchangeCodeRepository exchangeCodeRepository,
                                TokenService tokenService,
                                AuthService authService) {
        this.exchangeCodeRepository = exchangeCodeRepository;
        this.tokenService = tokenService;
        this.authService = authService;
    }

    /**
     * Creates a short-lived, single-use exchange code bound to an existing
     * Qedet AuthSession/token pair. The raw Qedet tokens are stored only in
     * this row and are never placed in redirect URLs.
     */
    @Transactional
    public String createCode(TokenService.TokenPair tokens) {
        if (tokens == null || tokens.accessToken() == null || tokens.refreshToken() == null) {
            throw new BadRequestException("Missing Qedet tokens for OAuth exchange");
        }
        AuthSession session = tokenService.authenticate(tokens.accessToken());
        ensureUserActive(session);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes());
        exchangeCodeRepository.save(new OAuthExchangeCode(
                session,
                TokenHasher.sha256(code),
                tokens.accessToken(),
                tokens.refreshToken(),
                Instant.now().plus(CODE_TTL)
        ));
        log.info("OAuth exchange created for userId={}", session.getUser().getId());
        return code;
    }

    /**
     * Consumes a single-use exchange code and returns the normal Qedet
     * {@link AuthResponse}. Organization/employee membership is optional.
     */
    @Transactional
    public AuthResponse exchange(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Missing OAuth exchange code");
        }

        OAuthExchangeCode exchangeCode = exchangeCodeRepository.findByCodeHash(TokenHasher.sha256(code))
                .orElseThrow(() -> new UnauthorizedException(
                        "INVALID_OAUTH_EXCHANGE_CODE",
                        "Invalid OAuth exchange code"
                ));

        Instant now = Instant.now();
        if (exchangeCode.getConsumedAt() != null) {
            throw new UnauthorizedException(
                    "OAUTH_EXCHANGE_CODE_USED",
                    "OAuth exchange code has already been used"
            );
        }
        if (exchangeCode.getExpiresAt() == null || !exchangeCode.getExpiresAt().isAfter(now)) {
            throw new UnauthorizedException(
                    "OAUTH_EXCHANGE_CODE_EXPIRED",
                    "OAuth exchange code has expired"
            );
        }

        String codeHash = exchangeCode.getCodeHash();
        String accessToken = exchangeCode.getAccessToken();
        String refreshToken = exchangeCode.getRefreshToken();
        if (accessToken == null || refreshToken == null || accessToken.isBlank() || refreshToken.isBlank()) {
            throw new UnauthorizedException(
                    "INVALID_OAUTH_EXCHANGE_CODE",
                    "Invalid OAuth exchange code"
            );
        }

        if (exchangeCodeRepository.consumeIfValid(codeHash, now, now) != 1) {
            throw new UnauthorizedException(
                    "OAUTH_EXCHANGE_CODE_USED",
                    "OAuth exchange code has already been used"
            );
        }

        // Scrub plaintext tokens from the consumed row; session remains valid for Bearer use.
        exchangeCodeRepository.clearStoredTokens(codeHash);

        AuthSession session = tokenService.authenticate(accessToken);
        ensureUserActive(session);

        log.info("OAuth exchange consumed successfully for userId={}", session.getUser().getId());
        return authService.authResponseForSession(
                session,
                new TokenService.TokenPair(accessToken, refreshToken)
        );
    }

    private static void ensureUserActive(AuthSession session) {
        if (session.getUser() == null || session.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is inactive");
        }
    }

    private byte[] randomBytes() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
