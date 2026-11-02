package q.qedet.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import q.qedet.auth.dto.GoogleIdentityClaims;
import q.qedet.auth.service.AuthService;
import q.qedet.auth.service.OAuthExchangeService;
import q.qedet.common.config.OAuthFlowAuthorizationRequestRepository;
import q.qedet.common.config.OAuthFlowAuthorizationRequestResolver;
import q.qedet.common.exception.ApiException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Completes Google OAuth by issuing a Qedet AuthSession and redirecting the
 * browser to the frontend OAuth callback with a single-use exchange code.
 *
 * <p>Frontend contract:
 * <pre>
 *   GET {qedet.frontend.base-url}{qedet.frontend.oauth-callback-path}?code={exchangeCode}
 *   POST /api/v1/auth/oauth/exchange  {"code":"..."}  → AuthResponse (same as login)
 * </pre>
 * Qedet access/refresh tokens are never placed in the redirect URL.
 */
@Component
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthSuccessHandler.class);

    private final AuthService authService;
    private final OAuthExchangeService oauthExchangeService;
    private final String frontendBaseUrl;
    private final String oauthCallbackPath;
    public GoogleOAuthSuccessHandler(
            AuthService authService,
            OAuthExchangeService oauthExchangeService,
            @Value("${qedet.frontend.base-url:http://localhost:5173}") String frontendBaseUrl,
            @Value("${qedet.frontend.oauth-callback-path:/auth/oauth/callback}") String oauthCallbackPath) {

        this.authService = authService;
        this.oauthExchangeService = oauthExchangeService;
        this.frontendBaseUrl = trimTrailingSlash(frontendBaseUrl);
        this.oauthCallbackPath = normalizePath(oauthCallbackPath);

        log.info("[GOOGLE-AUTH-CONFIG] frontendBaseUrl={}", this.frontendBaseUrl);
        log.info("[GOOGLE-AUTH-CONFIG] oauthCallbackPath={}", this.oauthCallbackPath);
    }
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        log.info("[GOOGLE-AUTH] STEP 3 - OAuth callback received");

        if (!(authentication.getPrincipal() instanceof OAuth2User user)) {
            redirectError(response, "GOOGLE_AUTH_FAILED");
            return;
        }

        Object email = user.getAttribute("email");
        Object sub = user.getAttribute("sub");
        Object emailVerified = user.getAttribute("email_verified");
        log.info("[GOOGLE-AUTH] STEP 4 - Google identity verified subjectPresent={}", sub instanceof String && !((String) sub).isBlank());

        if (!(email instanceof String googleEmail) || googleEmail.isBlank()
                || !(sub instanceof String googleSubject) || googleSubject.isBlank()
                || !Boolean.TRUE.equals(emailVerified)) {
            redirectError(response, "GOOGLE_AUTH_FAILED");
            return;
        }

        String flow = resolveFlow(request);
        log.info("Completing Qedet Google authentication for flow={}", flow);

        try {
            TokenService.TokenPair tokens = authService.authenticateGoogleUser(new GoogleIdentityClaims(
                    googleSubject,
                    googleEmail,
                    stringAttribute(user, "name"),
                    stringAttribute(user, "given_name"),
                    stringAttribute(user, "family_name"),
                    stringAttribute(user, "picture")
            ));

            String code = oauthExchangeService.createCode(tokens);

            clearGoogleAuthentication(request);

            String redirect = frontendBaseUrl
                    + oauthCallbackPath
                    + "?code="
                    + URLEncoder.encode(code, StandardCharsets.UTF_8);
            log.info("[GOOGLE-AUTH] STEP 7 - Sending authentication result to frontend");
            log.info("[GOOGLE-AUTH] STEP 7 - deliveryMechanism=redirect path={} (exchange code only; tokens omitted)",
                    oauthCallbackPath);
            response.sendRedirect(redirect);
        } catch (ApiException exception) {
            clearGoogleAuthentication(request);
            log.warn("Qedet Google {} failed code={}: {}", flow, exception.getCode(), exception.getMessage());
            redirectError(response, googleErrorCode(exception));
        } catch (RuntimeException exception) {
            clearGoogleAuthentication(request);
            log.error("Unexpected error completing Qedet Google authentication for email={}",
                    googleEmail, exception);
            redirectError(response, "GOOGLE_AUTH_FAILED");
        }
    }

    private String resolveFlow(HttpServletRequest request) {
        Object attribute = request.getAttribute(OAuthFlowAuthorizationRequestResolver.FLOW_ATTRIBUTE);
        if (attribute instanceof String attributeFlow && !attributeFlow.isBlank()) {
            return OAuthFlowAuthorizationRequestResolver.normalizeFlow(attributeFlow);
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object stored = session.getAttribute(OAuthFlowAuthorizationRequestRepository.FLOW_SESSION_ATTRIBUTE);
            if (stored instanceof String sessionFlow) {
                return OAuthFlowAuthorizationRequestResolver.normalizeFlow(sessionFlow);
            }
        }
        return OAuthFlowAuthorizationRequestResolver.normalizeFlow(null);
    }

    private void clearGoogleAuthentication(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private void redirectError(HttpServletResponse response, String error) throws IOException {
        log.debug("Redirecting Google OAuth failure to frontend login error={}", error);
        response.sendRedirect(
                frontendBaseUrl
                        + "/login?error="
                        + URLEncoder.encode(error, StandardCharsets.UTF_8)
        );
    }

    private String googleErrorCode(ApiException exception) {
        return "GOOGLE_AUTH_FAILED";
    }

    private static String stringAttribute(OAuth2User user, String name) {
        Object value = user.getAttribute(name);
        return value instanceof String string ? string : null;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:5173";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/auth/oauth/callback";
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
