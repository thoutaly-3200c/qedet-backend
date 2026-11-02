package q.qedet.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import q.qedet.auth.security.BearerTokenFilter;
import q.qedet.auth.security.GoogleOAuthSuccessHandler;
import q.qedet.auth.security.TokenService;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new org.springframework.security.core.userdetails.UsernameNotFoundException(username);
        };
    }

    @Bean
    public BearerTokenFilter bearerTokenFilter(TokenService tokenService,
                                               q.qedet.employee.repository.EmployeeRoleRepository employeeRoleRepository,
                                               q.qedet.employee.repository.RolePermissionRepository rolePermissionRepository) {
        return new BearerTokenFilter(tokenService, employeeRoleRepository, rolePermissionRepository);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   BearerTokenFilter bearerTokenFilter,
                                                   ApplicationContext applicationContext,
                                                   GoogleOAuthSuccessHandler googleOAuthSuccessHandler,
                                                   ObjectProvider<ClientRegistrationRepository> clientRegistrationProvider,
                                                   CorsConfigurationSource corsConfigurationSource,
                                                   @Value("${qedet.frontend.base-url:http://localhost:5173}") String frontendBaseUrl)
            throws Exception {
        String frontend = trimTrailingSlash(frontendBaseUrl);
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource));
        // OAuth2 authorization requests need a short-lived HTTP session. API
        // authentication remains bearer-token based because no API endpoint
        // writes a security context to that session.
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/resend-verification",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/oauth/exchange",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password",
                        "/api/v1/auth/verify-email",
                        "/oauth2/**",
                        "/login/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/invitations/*/accept").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/qr/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/jobs/*").permitAll()
                .anyRequest().authenticated());
        http.exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint()));
        if (applicationContext.containsBean("clientRegistrationRepository")) {
            ClientRegistrationRepository clientRegistrationRepository =
                    clientRegistrationProvider.getIfAvailable();
            http.oauth2Login(oauth -> oauth
                    .authorizationEndpoint(endpoint -> endpoint
                            .authorizationRequestResolver(
                                    new OAuthFlowAuthorizationRequestResolver(clientRegistrationRepository)
                            )
                            .authorizationRequestRepository(oauthAuthorizationRequestRepository())
                    )
                    .successHandler(googleOAuthSuccessHandler)
                    .failureHandler(
                            (request, response, exception) -> {

                                log.error("===== GOOGLE OAUTH FAILURE =====");
                                log.error("Exception type: {}", exception.getClass().getName());
                                log.error("Message: {}", exception.getMessage());

                                if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
                                    OAuth2Error error = oauth2Exception.getError();

                                    log.error("OAuth2 error code: {}", error.getErrorCode());
                                    log.error("OAuth2 error description: {}", error.getDescription());
                                    log.error("OAuth2 error URI: {}", error.getUri());
                                }

                                if (exception.getCause() != null) {
                                    log.error("Root cause:", exception.getCause());
                                }

                                log.error("================================");

                                response.sendRedirect(
                                        frontend + "/login?error=" +
                                                URLEncoder.encode("GOOGLE_AUTH_FAILED", StandardCharsets.UTF_8)
                                );
                            }
                    )
            );
        }
        http.addFilterBefore(bearerTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:5173";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> oauthAuthorizationRequestRepository() {
        return new OAuthFlowAuthorizationRequestRepository(
                new HttpSessionOAuth2AuthorizationRequestRepository()
        );
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
        };
    }
    @Bean
    CommandLineRunner debugFrontendConfig(
            @Value("${qedet.frontend.base-url:NOT_FOUND}") String frontendBaseUrl) {

        return args -> {
            log.info("========================================");
            log.info("[CONFIG-DEBUG] qedet.frontend.base-url = {}", frontendBaseUrl);
            log.info("========================================");
        };
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${qedet.frontend.base-url:http://localhost:5173}") String frontendBaseUrl,
            @Value("${qedet.frontend.cors-allowed-origins:https://localhost:5173,http://localhost:8443,https://192.168.0.103:8443}")
            String corsAllowedOrigins) {
        LinkedHashSet<String> origins = new LinkedHashSet<>();
        origins.add(trimTrailingSlash(frontendBaseUrl));
        if (corsAllowedOrigins != null && !corsAllowedOrigins.isBlank()) {
            Arrays.stream(corsAllowedOrigins.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isEmpty())
                    .forEach(origins::add);
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.copyOf(origins));
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
