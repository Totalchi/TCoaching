package be.vdab.tcoaching.config;

import be.vdab.tcoaching.api.client.PortalUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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

        PortalUserDetailsService service = new PortalUserDetailsService(
                mock(JdbcTemplate.class),
                PasswordEncoderFactories.createDelegatingPasswordEncoder(),
                environment,
                false
        );

        UserDetails user = service.loadUserByUsername("Totalchi");

        assertThat(user.getUsername()).isEqualTo("Totalchi");
        assertThat(PasswordEncoderFactories.createDelegatingPasswordEncoder().matches("coaching", user.getPassword())).isTrue();
    }
}
