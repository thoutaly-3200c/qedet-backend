package q.qedet.auth.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import q.qedet.auth.dto.ChangePasswordRequest;
import q.qedet.auth.dto.ForgotPasswordRequest;
import q.qedet.auth.dto.PasswordMessageResponse;
import q.qedet.auth.dto.ResetPasswordRequest;
import q.qedet.auth.dto.VerifyEmailRequest;
import q.qedet.auth.dto.VerifyEmailResponse;
import q.qedet.auth.dto.ResendVerificationCodeRequest;
import q.qedet.auth.security.AuthenticatedPrincipal;
import q.qedet.auth.security.ActorContext;
import q.qedet.auth.service.PasswordManagementService;
import q.qedet.auth.service.OAuthExchangeService;
import q.qedet.auth.dto.AuthRequest;
import q.qedet.auth.dto.AuthResponse;
import q.qedet.auth.dto.LogoutRequest;
import q.qedet.auth.dto.OAuthExchangeRequest;
import q.qedet.auth.dto.RefreshRequest;
import q.qedet.auth.dto.RegistrationResponse;
import q.qedet.auth.service.AuthService;
import q.qedet.organization.dto.RegistrationRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final PasswordManagementService passwordManagementService;
    private final OAuthExchangeService oauthExchangeService;
    private final ActorContext actorContext;

    public AuthController(AuthService authService,
                          PasswordManagementService passwordManagementService,
                          OAuthExchangeService oauthExchangeService,
                          ActorContext actorContext) {
        this.authService = authService;
        this.passwordManagementService = passwordManagementService;
        this.oauthExchangeService = oauthExchangeService;
        this.actorContext = actorContext;
    }

    @PostMapping("/register")
    public RegistrationResponse register(@Valid @RequestBody RegistrationRequest request) {
        return authService.registerUser(request);
    }

    @PostMapping("/verify-email")
    public VerifyEmailResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return authService.verifyEmail(request);
    }

    @PostMapping("/resend-verification")
    public PasswordMessageResponse resendVerificationCode(
            @Valid @RequestBody ResendVerificationCodeRequest request) {
        authService.resendVerificationCode(request.email());
        return new PasswordMessageResponse(
                "If the account exists and is not verified, a new verification code has been sent."
        );
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @GetMapping({"/me", "/current-user"})
    public AuthResponse currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean actorPresent = authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedPrincipal;
        Object userId = actorPresent
                ? ((AuthenticatedPrincipal) authentication.getPrincipal()).userId()
                : null;
        log.info("[AUTH] /auth/me current actor present={} userId={}", actorPresent, userId);
        return authService.currentUser(actorContext.current());
    }

    @PostMapping("/logout")
    public Map<String, String> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestBody(required = false) LogoutRequest request) {
        authService.logout(resolveBearer(authorization), request == null ? null : request.refreshToken());
        return Map.of("status", "ok");
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    /**
     * Exchanges a single-use OAuth browser code for the normal Qedet
     * {@link AuthResponse} (same shape as email/password login).
     *
     * <pre>
     * POST /api/v1/auth/oauth/exchange
     * {"code":"&lt;exchange-code&gt;"}
     * → AuthResponse { accessToken, refreshToken, userId, employeeId?, organizationId?, ... }
     * </pre>
     */
    @PostMapping("/oauth/exchange")
    public AuthResponse exchangeOAuthCode(@Valid @RequestBody OAuthExchangeRequest request) {
        return oauthExchangeService.exchange(request.code());
    }

    @PostMapping("/forgot-password")
    public PasswordMessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                   HttpServletRequest servletRequest) {
        return new PasswordMessageResponse(passwordManagementService.requestReset(request, clientIp(servletRequest)));
    }

    @PostMapping("/reset-password")
    public PasswordMessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                                  HttpServletRequest servletRequest) {
        passwordManagementService.resetPassword(request, clientIp(servletRequest));
        return new PasswordMessageResponse("Password has been reset successfully.");
    }

    @PostMapping("/change-password")
    public PasswordMessageResponse changePassword(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                                   @Valid @RequestBody ChangePasswordRequest request) {
        passwordManagementService.changePassword(principal.userId(), request);
        return new PasswordMessageResponse("Password changed successfully.");
    }

    private String resolveBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }
}
