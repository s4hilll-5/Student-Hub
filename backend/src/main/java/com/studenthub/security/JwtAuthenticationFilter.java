package com.studenthub.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.studenthub.repository.UserRepository;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reads "Authorization: Bearer &lt;token&gt;", verifies it and puts the user into the
 * SecurityContext. The user row is re-loaded on each request (a primary-key lookup) so
 * that disabled accounts and changed passwords take effect immediately: the token's
 * "ver" claim must equal the user's current token_version.
 *
 * Invalid tokens are not rejected here; the request simply stays anonymous and Spring
 * Security's entry point answers 401 if the endpoint needs authentication.
 *
 * Not a Spring bean on purpose: it is created in SecurityConfig so the servlet container
 * does not register it a second time outside the security filter chain.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTRIBUTE = "studenthub.authError";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            JwtService.AccessTokenClaims claims = jwtService.parseAccessToken(token);
            userRepository.findById(claims.userId())
                    .filter(user -> user.isEnabled())
                    .filter(user -> user.getTokenVersion() == claims.tokenVersion())
                    .ifPresentOrElse(user -> {
                        AppUserPrincipal principal = AppUserPrincipal.withoutCredentials(user);
                        UsernamePasswordAuthenticationToken authentication =
                                UsernamePasswordAuthenticationToken.authenticated(
                                        principal, null, principal.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContext context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(authentication);
                        SecurityContextHolder.setContext(context);
                    }, () -> request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Your session is no longer valid. Sign in again."));
        } catch (ExpiredJwtException e) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Your session has expired. Sign in again.");
        } catch (JwtException | IllegalArgumentException e) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Invalid access token.");
        }

        chain.doFilter(request, response);
    }
}
