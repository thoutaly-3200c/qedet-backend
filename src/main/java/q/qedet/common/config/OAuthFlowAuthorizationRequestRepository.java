package q.qedet.common.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

public class OAuthFlowAuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String FLOW_SESSION_ATTRIBUTE =
            OAuthFlowAuthorizationRequestResolver.FLOW_ATTRIBUTE + ".session";

    private final AuthorizationRequestRepository<OAuth2AuthorizationRequest> delegate;

    public OAuthFlowAuthorizationRequestRepository(
            AuthorizationRequestRepository<OAuth2AuthorizationRequest> delegate) {
        this.delegate = delegate;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return delegate.loadAuthorizationRequest(request);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        delegate.saveAuthorizationRequest(authorizationRequest, request, response);
        if (authorizationRequest != null) {
            request.getSession().setAttribute(
                    FLOW_SESSION_ATTRIBUTE,
                    flowFrom(authorizationRequest)
            );
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest =
                delegate.removeAuthorizationRequest(request, response);
        String flow = authorizationRequest != null ? flowFrom(authorizationRequest) : null;
        HttpSession session = request.getSession(false);
        if (flow == null && session != null) {
            Object stored = session.getAttribute(FLOW_SESSION_ATTRIBUTE);
            if (stored instanceof String sessionFlow) {
                flow = sessionFlow;
            }
        }
        // Keep the session copy until GoogleOAuthSuccessHandler finishes and
        // invalidates the HTTP session. Never trust a callback query parameter.
        String normalized = OAuthFlowAuthorizationRequestResolver.normalizeFlow(flow);
        request.setAttribute(OAuthFlowAuthorizationRequestResolver.FLOW_ATTRIBUTE, normalized);
        if (session != null) {
            session.setAttribute(FLOW_SESSION_ATTRIBUTE, normalized);
        }
        return authorizationRequest;
    }

    private static String flowFrom(OAuth2AuthorizationRequest authorizationRequest) {
        return OAuthFlowAuthorizationRequestResolver.normalizeFlow(
                (String) authorizationRequest.getAttribute(
                        OAuthFlowAuthorizationRequestResolver.FLOW_ATTRIBUTE
                )
        );
    }
}
