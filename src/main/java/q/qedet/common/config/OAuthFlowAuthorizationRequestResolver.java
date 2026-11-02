package q.qedet.common.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.HashMap;
import java.util.Map;

public class OAuthFlowAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    public static final String FLOW_ATTRIBUTE = OAuthFlowAuthorizationRequestResolver.class.getName() + ".flow";
    private static final String LOGIN_FLOW = "login";
    private static final String SIGNUP_FLOW = "signup";

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public OAuthFlowAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                "/oauth2/authorization"
        );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return withFlow(request, delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return withFlow(request, delegate.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest withFlow(HttpServletRequest request,
                                                OAuth2AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null) {
            return null;
        }
        Map<String, Object> attributes = new HashMap<>(authorizationRequest.getAttributes());
        attributes.put(FLOW_ATTRIBUTE, normalizeFlow(request.getParameter("flow")));
        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .attributes(attributes)
                .build();
    }

    public static String normalizeFlow(String flow) {
        return SIGNUP_FLOW.equalsIgnoreCase(flow) ? SIGNUP_FLOW : LOGIN_FLOW;
    }
}
