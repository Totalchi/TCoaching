package be.vdab.tcoaching.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigCredentialResolutionTests {

    @Test
    void localAdminCredentialsWinWhenHigherPriorityEnvironmentVariablesAreBlank() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("blank-env", Map.of(
                "ADMIN_USER", "",
                "ADMIN_PASSWORD", ""
        )));
        environment.getPropertySources().addAfter("blank-env", new MapPropertySource("application", Map.of(
                "security.admin.username", "${ADMIN_USER:}",
                "security.admin.password", "${ADMIN_PASSWORD:}"
        )));
        environment.getPropertySources().addLast(new MapPropertySource("local-file", Map.of(
                "ADMIN_USER", "Totalchi",
                "ADMIN_PASSWORD", "coaching"
        )));

        SecurityConfig securityConfig = new SecurityConfig(environment);
        InMemoryUserDetailsManager manager =
                (InMemoryUserDetailsManager) securityConfig.userDetailsService(false, securityConfig.passwordEncoder());

        UserDetails user = manager.loadUserByUsername("Totalchi");

        assertThat(user.getUsername()).isEqualTo("Totalchi");
        assertThat(securityConfig.passwordEncoder().matches("coaching", user.getPassword())).isTrue();
    }
}
