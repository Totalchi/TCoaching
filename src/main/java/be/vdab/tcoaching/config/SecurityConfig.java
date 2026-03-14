package be.vdab.tcoaching.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class SecurityConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);
    private static final String[] PUBLIC_PAGES = {
            "/",
            "/index.html",
            "/about.html",
            "/inzichten.html",
            "/privacy.html",
            "/life-coaching.html",
            "/personal-training.html",
            "/stress-burnout.html",
            "/assertiviteit.html",
            "/prijzen.html",
            "/contact.html",
            "/robots.txt",
            "/sitemap.xml",
            "/site.webmanifest",
            "/favicon.ico"
    };
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "change-me";
    private static final String CONTENT_SECURITY_POLICY =
            "default-src 'self'; " +
            "script-src 'self' https://challenges.cloudflare.com; " +
            "style-src 'self' https://fonts.googleapis.com; " +
            "font-src 'self' https://fonts.gstatic.com; " +
            "img-src 'self' data: https://challenges.cloudflare.com; " +
            "connect-src 'self' https://challenges.cloudflare.com; " +
            "frame-src 'self' https://challenges.cloudflare.com https://calendly.com https://*.calendly.com https://app.acuityscheduling.com; " +
            "object-src 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self'; " +
            "frame-ancestors 'none'";

    private static final String PERMISSIONS_POLICY =
            "accelerometer=(), camera=(), geolocation=(), gyroscope=(), magnetometer=(), " +
            "microphone=(), payment=(), usb=(), fullscreen=(self)";

    private final Environment environment;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
            @Value("${security.admin.username}") String username,
            @Value("${security.admin.password}") String password,
            @Value("${security.admin.require-non-default:false}") boolean requireNonDefault,
            PasswordEncoder passwordEncoder
    ) {
        boolean hasUsername = username != null && !username.isBlank();
        boolean hasPassword = password != null && !password.isBlank();
        if (!hasUsername || !hasPassword) {
            LOGGER.info("No admin credentials configured; authenticated routes remain unavailable.");
            return new InMemoryUserDetailsManager();
        }

        boolean defaultUsername = DEFAULT_ADMIN_USERNAME.equals(username);
        boolean defaultPassword = DEFAULT_ADMIN_PASSWORD.equals(password);
        if (requireNonDefault && (defaultUsername || defaultPassword)) {
            throw new IllegalStateException("Default admin credentials are not allowed.");
        }
        if (defaultUsername || defaultPassword) {
            LOGGER.warn("Default admin credentials are in use. Set ADMIN_USER and ADMIN_PASSWORD.");
        }
        String encodedPassword = password.startsWith("{") ? password : passwordEncoder.encode(password);
        return new InMemoryUserDetailsManager(
                User.withUsername(username)
                        .password(encodedPassword)
                        .roles("ADMIN")
                        .build()
        );
    }

    @Bean
    @SuppressWarnings("deprecation")
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RateLimitingFilter rateLimitingFilter
    ) {
        boolean requireHttps = environment.acceptsProfiles(Profiles.of("prod"));
        if (requireHttps) {
            http.requiresChannel((channel) -> channel.anyRequest().requiresSecure());
        }
        http
                .addFilterBefore(rateLimitingFilter, SecurityContextHolderFilter.class)
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers(HttpMethod.GET, PUBLIC_PAGES).permitAll()
                        .requestMatchers(HttpMethod.HEAD, PUBLIC_PAGES).permitAll()
                        .requestMatchers(HttpMethod.GET, "/assets/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/assets/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public-config").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/api/public-config").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/contact", "/api/track").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf((csrf) -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/contact", "/api/track")
                )
                .httpBasic(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .headers((headers) -> headers
                        .addHeaderWriter(new ContentSecurityPolicyHeaderWriter(CONTENT_SECURITY_POLICY))
                        .addHeaderWriter(new ReferrerPolicyHeaderWriter(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", PERMISSIONS_POLICY))
                        .addHeaderWriter(new StaticHeadersWriter("X-Content-Type-Options", "nosniff"))
                        .addHeaderWriter(new StaticHeadersWriter("Cross-Origin-Opener-Policy", "same-origin"))
                        .addHeaderWriter(new StaticHeadersWriter("Cross-Origin-Resource-Policy", "same-origin"))
                        .addHeaderWriter(new StaticHeadersWriter("X-Permitted-Cross-Domain-Policies", "none"))
                        .frameOptions(org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig::deny)
                        .httpStrictTransportSecurity((hsts) -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000)
                        )
                );

        return http.build();
    }
}
