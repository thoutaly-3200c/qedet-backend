package q.qedet.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import q.qedet.common.exception.ExpiredResetTokenException;
import q.qedet.common.exception.UnauthorizedException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/** Owns password-reset credentials and password changes; controllers never handle secrets. */
@Service
public class PasswordManagementService {

    private static final Logger log = LoggerFactory.getLogger(PasswordManagementService.class);
    public static final String FORGOT_PASSWORD_MESSAGE =
            "If an account exists with this email, a password reset link has been sent.";
    public static final String INVALID_RESET_TOKEN_MESSAGE =
            "The password reset link is invalid or has expired.";
    public static final String EXPIRED_RESET_TOKEN_MESSAGE =
            "The password reset link has expired. Please request a new one.";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(20);
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(15);

    private final UserAccountRepository userAccountRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;

    public PasswordManagementService(UserAccountRepository userAccountRepository,
                                     PasswordResetTokenRepository passwordResetTokenRepository,
                                     PasswordEncoder passwordEncoder,
                                     PasswordPolicy passwordPolicy,
                                     TokenService tokenService,
                                     EmailService emailService,
                                     RateLimitService rateLimitService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.rateLimitService = rateLimitService;
    }

    /**
     * Always returns the same message so an email address cannot be used to enumerate accounts.
     * Raw reset tokens are only ever present in this method and the email-service call.
     */
    @Transactional
    public String requestReset(ForgotPasswordRequest request, String clientIp) {
        String email = request.email().trim().toLowerCase();
        log.info("Received password reset request for email={}", email);
        
        log.info("Checking email rate limit for email={}", email);
        rateLimitService.requireAllowed("password-reset:email:" + email, 3, RATE_LIMIT_WINDOW,
                "Too many password reset requests. Please try again later.");
        log.info("Email rate limit check passed for email={}", email);
        
        log.info("Checking IP rate limit for ip={}", normalizedIp(clientIp));
        rateLimitService.requireAllowed("password-reset:ip:" + normalizedIp(clientIp), 10, RATE_LIMIT_WINDOW,
                "Too many password reset requests. Please try again later.");
        log.info("IP rate limit check passed for ip={}", normalizedIp(clientIp));

        log.info("Looking up user account for email={}", email);
        userAccountRepository.findByEmailIgnoreCase(email).ifPresentOrElse(
                user -> {
                    log.info("User account found for email={}, proceeding with token creation", email);
                    createAndSendResetToken(user, email);
                },
                () -> log.info("No user account found for email={}, skipping token creation", email)
        );
        
        log.info("Returning standard response message for password reset request");
        return FORGOT_PASSWORD_MESSAGE;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request, String clientIp) {
        rateLimitService.requireAllowed("password-reset-submit:ip:" + normalizedIp(clientIp), 10, RATE_LIMIT_WINDOW,
                "Too many password reset attempts. Please try again later.");
        requireMatchingPasswords(request.newPassword(), request.confirmPassword());
        passwordPolicy.validate(request.newPassword());

        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(TokenHasher.sha256(request.token()))
                .orElseThrow(this::invalidResetToken);
        Instant now = Instant.now();
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) {
            if (!token.getExpiresAt().isAfter(now)) {
                throw new ExpiredResetTokenException(EXPIRED_RESET_TOKEN_MESSAGE);
            }
            throw invalidResetToken();
        }

        UserAccount user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.setUsedAt(now);
        userAccountRepository.save(user);
        passwordResetTokenRepository.save(token);
        tokenService.revokeAllForUser(user.getId());
    }

    @Transactional
    public void changePassword(java.util.UUID userId, ChangePasswordRequest request) {
        requireMatchingPasswords(request.newPassword(), request.confirmPassword());
        passwordPolicy.validate(request.newPassword());

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userAccountRepository.save(user);
        tokenService.revokeAllForUser(user.getId());
    }

    private void createAndSendResetToken(UserAccount user, String email) {
        log.info("Starting password reset token creation for userId={}", user.getId());
        Instant now = Instant.now();
        log.info("Invalidating active password reset tokens for userId={}", user.getId());
        passwordResetTokenRepository.findAllByUserIdAndUsedAtIsNullAndExpiresAtAfter(user.getId(), now)
                .forEach(previous -> previous.setUsedAt(now));

        log.info("Generating password reset token for userId={}", user.getId());
        String rawToken = generateRawToken();
        log.info("Saving password reset token hash for userId={} with expiry={}",
                user.getId(), now.plus(RESET_TOKEN_TTL));
        passwordResetTokenRepository.save(new PasswordResetToken(
                user, TokenHasher.sha256(rawToken), now.plus(RESET_TOKEN_TTL)));
        log.info("Password reset token saved for userId={}", user.getId());
        try {
            log.info("Sending password reset email to recipient={}", email);
            emailService.sendPasswordResetEmail(email, rawToken);
            log.info("Password reset email sent to recipient={}", email);
        } catch (RuntimeException exception) {
            log.warn("Password reset email delivery failed for recipient={}", email, exception);
        }
        log.info("Completed password reset token workflow for userId={}", user.getId());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void requireMatchingPasswords(String password, String confirmation) {
        if (!password.equals(confirmation)) {
            throw new BadRequestException("Password confirmation does not match.");
        }
    }

    private BadRequestException invalidResetToken() {
        return new BadRequestException(INVALID_RESET_TOKEN_MESSAGE);
    }

    private String normalizedIp(String clientIp) {
        return clientIp == null || clientIp.isBlank() ? "unknown" : clientIp;
    }
}
