package com.portfolio.app.security;

import com.portfolio.identity.internal.config.IdentityProperties;
import com.portfolio.identity.web.GoogleLoginHandlers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.time.Duration;
import java.util.List;

/**
 * HTTP security for the whole application.
 *
 * <ul>
 *   <li>Stateless API: every request carries a short-lived JWT in the Authorization header.</li>
 *   <li>CSRF tokens are not needed for bearer-token requests (browsers never attach that header on their
 *       own). The two cookie-authenticated endpoints are protected by SameSite=Strict + an Origin check.</li>
 *   <li>Deny by default: anything not explicitly listed is rejected.</li>
 *   <li>Coarse URL rules here, fine-grained {@code @PreAuthorize} rules on controllers and services.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** API, Google sign-in endpoints and error dispatches. Everything else goes to {@link #webSecurity}. */
    @Bean
    @Order(1)
    SecurityFilterChain apiSecurity(HttpSecurity http,
                                    IdentityProperties props,
                                    ObjectProvider<ClientRegistrationRepository> clientRegistrations,
                                    GoogleLoginHandlers googleHandlers,
                                    JsonSecurityErrorHandlers jsonErrors,
                                    RateLimitProperties rateLimitProperties) throws Exception {
        http
                .securityMatcher("/api/**", "/oauth2/**", "/login/oauth2/**", "/error")
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/profile", "/api/posts", "/api/posts/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/oauth2/authorization/**", "/login/oauth2/code/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(rs -> rs
                        .bearerTokenResolver(bearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(jsonErrors)
                        .accessDeniedHandler(jsonErrors))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(jsonErrors)
                        .accessDeniedHandler(jsonErrors))
                .headers(h -> h
                        // The API only returns JSON: forbid everything a browser could do with a response.
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"))
                        .frameOptions(f -> f.deny())
                        .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                                "camera=(), microphone=(), geolocation=(), payment=()"))
                        .addHeaderWriter(new StaticHeadersWriter("Cross-Origin-Opener-Policy", "same-origin")))
                .addFilterAfter(new RateLimitFilter(rateLimitProperties), CorsFilter.class);

        ClientRegistrationRepository registrations = clientRegistrations.getIfAvailable();
        if (registrations != null) {
            DefaultOAuth2AuthorizationRequestResolver resolver =
                    new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
            // PKCE on top of client secret + state + nonce: an intercepted authorization code is useless.
            resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
            http.oauth2Login(o -> o
                    .loginPage(props.getFrontendUrl() + "/login")
                    .authorizationEndpoint(a -> a.authorizationRequestResolver(resolver))
                    .successHandler(googleHandlers)
                    .failureHandler(googleHandlers));
        }
        return http.build();
    }

    /**
     * The React single-page app (index.html, /assets/**, client-side routes) served by Spring Boot in
     * production. Read-only: GET/HEAD only, everything else is denied. Real access control lives on the API.
     */
    @Bean
    @Order(2)
    SecurityFilterChain webSecurity(HttpSecurity http, JsonSecurityErrorHandlers jsonErrors) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/**").permitAll()
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(jsonErrors)
                        .accessDeniedHandler(jsonErrors))
                .headers(h -> h
                        // Same policy as `vite preview`: only this origin's scripts, Google Fonts for styles/fonts.
                        .contentSecurityPolicy(csp -> csp.policyDirectives(String.join("; ",
                                "default-src 'self'",
                                "script-src 'self'",
                                "style-src 'self' https://fonts.googleapis.com",
                                "font-src 'self' https://fonts.gstatic.com",
                                "img-src 'self' https: data:",
                                "connect-src 'self'",
                                "object-src 'none'",
                                "base-uri 'self'",
                                "form-action 'self'",
                                "frame-ancestors 'none'")))
                        .frameOptions(f -> f.deny())
                        .referrerPolicy(r -> r.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                                "camera=(), microphone=(), geolocation=(), payment=()")));
        return http.build();
    }

    /** Ignore Authorization headers on /api/auth/**: an expired token must never block refresh or login. */
    private static BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
        return request -> request.getRequestURI().startsWith("/api/auth/") ? null : delegate.resolve(request);
    }

    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(IdentityProperties props) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(props.getAllowedOrigins()); // explicit list, never "*" together with credentials
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }
}
