package be.vdab.tcoaching.api.client;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("LombokGetterMayBeUsed")
public final class ClientPrincipal implements UserDetails, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final long clientId;
    private final String email;
    private final String passwordHash;
    private final String firstName;
    private final String lastName;
    private final String lang;
    private final boolean enabled;

    public ClientPrincipal(
            long clientId,
            String email,
            String passwordHash,
            String firstName,
            String lastName,
            String lang,
            boolean enabled
    ) {
        this.clientId = clientId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.lang = lang;
        this.enabled = enabled;
    }

    public long getClientId() {
        return clientId;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getLang() {
        return lang;
    }

    public String getFullName() {
        String combined = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        return combined.isEmpty() ? email : combined;
    }

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_CLIENT"));
    }

    @Override
    public @NonNull String getPassword() {
        return passwordHash;
    }

    @Override
    public @NonNull String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
