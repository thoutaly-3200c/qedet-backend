package q.qedet.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import q.qedet.auth.dto.GoogleIdentityClaims;
import q.qedet.auth.service.AuthService;
import q.qedet.auth.service.OAuthExchangeService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleOAuthSuccessHandlerTest {

    @Test
    void redirectsWithOnlyExchangeCodeAndPassesVerifiedGoogleClaimsToAuthService() throws Exception {
        AuthService authService = mock(AuthService.class);
        OAuthExchangeService exchangeService = mock(OAuthExchangeService.class);
        GoogleOAuthSuccessHandler handler = new GoogleOAuthSuccessHandler(
                authService, exchangeService, "https://app.qedet.test/", "/auth/oauth/callback"
        );
        OAuth2User user = mock(OAuth2User.class);
        Authentication authentication = mock(Authentication.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(authentication.getPrincipal()).thenReturn(user);
        when(user.getAttribute("email")).thenReturn("user@example.com");
        when(user.getAttribute("sub")).thenReturn("google-subject");
        when(user.getAttribute("email_verified")).thenReturn(true);
        when(user.getAttribute("name")).thenReturn("Google User");
        when(user.getAttribute("given_name")).thenReturn("Google");
        when(user.getAttribute("family_name")).thenReturn("User");
        when(user.getAttribute("picture")).thenReturn("https://example.com/picture.png");
        when(authService.authenticateGoogleUser(any())).thenReturn(new TokenService.TokenPair("access-token", "refresh-token"));
        when(exchangeService.createCode(any())).thenReturn("single-use-code");

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<GoogleIdentityClaims> claims = ArgumentCaptor.forClass(GoogleIdentityClaims.class);
        verify(authService).authenticateGoogleUser(claims.capture());
        assertThat(claims.getValue().subject()).isEqualTo("google-subject");
        assertThat(claims.getValue().email()).isEqualTo("user@example.com");
        ArgumentCaptor<String> redirect = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirect.capture());
        assertThat(redirect.getValue()).isEqualTo("https://app.qedet.test/auth/oauth/callback?code=single-use-code");
        assertThat(redirect.getValue()).doesNotContain("access-token", "refresh-token");
    }
}
