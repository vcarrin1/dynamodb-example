package com.vcarrin87.dynamodb_example.config;

import static org.springframework.security.config.Customizer.withDefaults;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String MESSAGE_ACCESS_DENIED = "Access Denied";
    private static final String MESSAGE_MISSING_TOKEN = "Missing token";
    private static final String MESSAGE_INVALID_TOKEN = "Invalid token";

    /**
     * Builds the HTTP security filter chain for the application.
     *
     * @param http Spring Security HTTP builder
     * @return configured filter chain
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/graphiql/**", "/graphiql", "/actuator/**", "/static/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS).permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions ->
                exceptions
                    .authenticationEntryPoint((request, response, authException) -> {
                        if (isGraphQlRequest(request)) {
                            writeGraphQlSecurityError(request, response, authException);
                            return;
                        }
                        new BearerTokenAuthenticationEntryPoint().commence(request, response, authException);
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        if (isGraphQlRequest(request)) {
                            writeGraphQlSecurityError(request, response, accessDeniedException);
                            return;
                        }
                        new BearerTokenAccessDeniedHandler().handle(request, response, accessDeniedException);
                    })
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationEntryPoint((request, response, authException) -> {
                    if (isGraphQlRequest(request)) {
                        writeGraphQlSecurityError(request, response, authException);
                        return;
                    }
                    new BearerTokenAuthenticationEntryPoint().commence(request, response, authException);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (isGraphQlRequest(request)) {
                        writeGraphQlSecurityError(request, response, accessDeniedException);
                        return;
                    }
                    new BearerTokenAccessDeniedHandler().handle(request, response, accessDeniedException);
                })
                .jwt(withDefaults())
            );

        return http.build();
    }

    /**
     * Configures CORS rules for local development clients.
     *
     * @return CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:8080", "http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Detects requests routed to the GraphQL endpoint.
     */
    private static boolean isGraphQlRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/graphql");
    }

    /**
     * Writes a GraphQL-compliant security payload for GraphiQL clients.
     */
    private static void writeGraphQlSecurityError(HttpServletRequest request, HttpServletResponse response,
            Exception exception) throws IOException {
        String message = resolveGraphQlSecurityMessage(request, exception);

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"errors\":[{\"message\":\"" + jsonEscape(message) + "\"}]}");
    }

    /**
     * Resolves distinct GraphQL auth messages for invalid or missing tokens.
     */
    private static String resolveGraphQlSecurityMessage(HttpServletRequest request, Exception exception) {
        if (!hasBearerToken(request)) {
            return MESSAGE_MISSING_TOKEN;
        }

        if (exception instanceof OAuth2AuthenticationException oauth2AuthenticationException
                && oauth2AuthenticationException.getError() != null
                && "invalid_token".equals(oauth2AuthenticationException.getError().getErrorCode())) {
            return MESSAGE_INVALID_TOKEN;
        }

        return MESSAGE_ACCESS_DENIED;
    }

    private static boolean hasBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        return authorization != null && authorization.startsWith(BEARER_PREFIX);
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
