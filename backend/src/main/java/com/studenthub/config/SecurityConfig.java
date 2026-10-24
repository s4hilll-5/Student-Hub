package com.studenthub.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

import com.studenthub.repository.UserRepository;
import com.studenthub.security.FixedWindowRateLimiter;
import com.studenthub.security.JsonErrorWriter;
import com.studenthub.security.JwtAuthenticationFilter;
import com.studenthub.security.JwtService;
import com.studenthub.security.RateLimitFilter;
import com.studenthub.security.RestAccessDeniedHandler;
import com.studenthub.security.RestAuthenticationEntryPoint;
import com.studenthub.security.SpaCsrfTokenRequestHandler;

/**
 * Stateless JWT security.
 *
 * CSRF, deliberately configured rather than switched off:
 *  - Normal API calls authenticate with an Authorization header that browsers never add
 *    automatically, so a forged cross-site request carries no credentials. CSRF tokens
 *    are not required for them.
 *  - The refresh token is an ambient credential (a cookie). The only endpoints that read
 *    it, POST /api/auth/refresh and /api/auth/logout, therefore require a CSRF token
 *    (double-submit: XSRF-TOKEN cookie copied into the X-XSRF-TOKEN header), on top of
 *    SameSite=Strict on the cookie itself.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Requests that are authenticated by the refresh cookie and therefore need CSRF protection. */
    static final RequestMatcher COOKIE_AUTHENTICATED_ENDPOINTS = request -> {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/api/auth/refresh") || path.equals("/api/auth/logout");
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            UserRepository userRepository,
            FixedWindowRateLimiter rateLimiter,
            JsonErrorWriter errorWriter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            CorsConfigurationSource corsConfigurationSource,
            AppProperties properties) throws Exception {

        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieCustomizer(cookie -> cookie
                .sameSite("Strict")
                .secure(properties.cookie().secure())
                .path("/"));

        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userRepository);
        RateLimitFilter rateLimitFilter = new RateLimitFilter(rateLimiter, errorWriter, properties.rateLimit().enabled());

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        .requireCsrfProtectionMatcher(COOKIE_AUTHENTICATED_ENDPOINTS))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .frameOptions(frame -> frame.deny()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(AppProperties properties) {
        return new BCryptPasswordEncoder(properties.security().bcryptStrength());
    }

    /**
     * Spring Security builds this from our UserDetailsService (AppUserDetailsService) and
     * PasswordEncoder beans. Its DaoAuthenticationProvider also compares against a dummy
     * hash when the email does not exist, so response time does not reveal accounts.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public FixedWindowRateLimiter fixedWindowRateLimiter(Clock clock) {
        return new FixedWindowRateLimiter(clock);
    }
}
