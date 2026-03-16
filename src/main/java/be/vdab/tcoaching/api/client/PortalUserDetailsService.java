package be.vdab.tcoaching.api.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.jspecify.annotations.NonNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

@Service
@SuppressWarnings({"SqlNoDataSourceInspection", "SqlDialectInspection"})
public class PortalUserDetailsService implements UserDetailsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortalUserDetailsService.class);
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "change-me";
    private static final String CLIENT_LOOKUP_SQL = """
            SELECT id, email, password_hash, first_name, last_name, lang, verified, active
            FROM clients
            WHERE LOWER(email) = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public PortalUserDetailsService(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            Environment environment,
            @Value("${security.admin.require-non-default:false}") boolean requireNonDefault
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = resolveFirstNonBlankProperty(environment, "security.admin.username", "ADMIN_USER");
        this.adminPassword = resolveFirstNonBlankProperty(environment, "security.admin.password", "ADMIN_PASSWORD");

        if (adminUsername.isBlank() || adminPassword.isBlank()) {
            LOGGER.info("No admin credentials configured; authenticated admin routes remain unavailable.");
            return;
        }

        boolean defaultUsername = DEFAULT_ADMIN_USERNAME.equals(adminUsername);
        boolean defaultPassword = DEFAULT_ADMIN_PASSWORD.equals(adminPassword);
        if (requireNonDefault && (defaultUsername || defaultPassword)) {
            throw new IllegalStateException("Default admin credentials are not allowed.");
        }
        if (defaultUsername || defaultPassword) {
            LOGGER.warn("Default admin credentials are in use. Set ADMIN_USER and ADMIN_PASSWORD.");
        }
    }

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        String normalizedUsername = normalize(username);
        if (!adminUsername.isBlank() && adminUsername.equals(username)) {
            String encodedPassword = adminPassword.startsWith("{")
                    ? adminPassword
                    : passwordEncoder.encode(adminPassword);
            return User.withUsername(adminUsername)
                    .password(encodedPassword)
                    .roles("ADMIN")
                    .build();
        }

        try {
            return jdbcTemplate.queryForObject(CLIENT_LOOKUP_SQL, this::mapClientPrincipal, normalizedUsername);
        } catch (EmptyResultDataAccessException ex) {
            throw new UsernameNotFoundException("No user found for " + username, ex);
        }
    }

    private ClientPrincipal mapClientPrincipal(ResultSet rs, int rowNum) throws SQLException {
        boolean verified = rs.getBoolean("verified");
        boolean active = rs.getBoolean("active");
        return new ClientPrincipal(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("lang"),
                verified && active
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveFirstNonBlankProperty(Environment environment, String... keys) {
        if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
            for (PropertySource<?> propertySource : configurableEnvironment.getPropertySources()) {
                for (String key : keys) {
                    String value = asConfiguredValue(propertySource.getProperty(key));
                    if (value != null) {
                        return value;
                    }
                }
            }
        }
        for (String key : keys) {
            String value = asConfiguredValue(environment.getProperty(key));
            if (value != null) {
                return value;
            }
        }
        return "";
    }

    private String asConfiguredValue(Object candidate) {
        if (candidate == null) {
            return null;
        }
        String value = String.valueOf(candidate);
        if (value.isBlank() || value.contains("${")) {
            return null;
        }
        return value;
    }
}
