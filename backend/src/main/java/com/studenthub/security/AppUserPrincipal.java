package com.studenthub.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.studenthub.entity.Role;
import com.studenthub.entity.User;

/**
 * The authenticated user as seen by Spring Security. Controllers receive it through
 * {@code @AuthenticationPrincipal} and pass {@link #getId()} to services, which is how
 * data ownership is derived from the security context instead of from request input.
 */
public class AppUserPrincipal implements UserDetails, CredentialsContainer {

    private final Long id;
    private final String email;
    private String passwordHash;
    private final Role role;
    private final boolean enabled;
    private final int tokenVersion;

    public AppUserPrincipal(Long id, String email, String passwordHash, Role role, boolean enabled, int tokenVersion) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
        this.tokenVersion = tokenVersion;
    }

    /** For password login (includes the hash so DaoAuthenticationProvider can check it). */
    public static AppUserPrincipal withCredentials(User user) {
        return new AppUserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRole(),
                user.isEnabled(), user.getTokenVersion());
    }

    /** For JWT-authenticated requests (no password needed). */
    public static AppUserPrincipal withoutCredentials(User user) {
        return new AppUserPrincipal(user.getId(), user.getEmail(), null, user.getRole(),
                user.isEnabled(), user.getTokenVersion());
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }
}
