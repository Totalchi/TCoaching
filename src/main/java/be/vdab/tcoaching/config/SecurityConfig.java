package be.vdab.tcoaching.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

@Configuration
public class SecurityConfig {
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
            "/favicon.ico",
            "/inloggen.html",
            "/registreer.html",
            "/wachtwoord-reset.html"
    };

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
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    @Order(1)
    @SuppressWarnings("deprecation")
    SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            RateLimitingFilter rateLimitingFilter,
            SecurityContextRepository securityContextRepository
    ) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            http.requiresChannel((channel) -> channel.anyRequest().requiresSecure());
        }

        http
                .securityMatcher("/api/**", "/actuator/**")
                .addFilterBefore(rateLimitingFilter, SecurityContextHolderFilter.class)
                .securityContext((security) -> security.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers(HttpMethod.GET, "/api/public-config", "/api/csrf").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/api/public-config", "/api/csrf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/contact", "/api/track").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/client/register",
                                "/api/client/login",
                                "/api/client/reset-password/request",
                                "/api/client/reset-password/confirm").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/client/verify-email").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/client/**").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .csrf((csrf) -> csrf
                        .csrfTokenRepository(httpOnlyCsrfRepository())
                        .ignoringRequestMatchers("/api/contact", "/api/track")
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling((exceptions) -> exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        applySecurityHeaders(http);
        return http.build();
    }

    @Bean
    @Order(2)
    @SuppressWarnings("deprecation")
    SecurityFilterChain webSecurityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository
    ) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            http.requiresChannel((channel) -> channel.anyRequest().requiresSecure());
        }

        http
                .securityMatcher("/**")
                .securityContext((security) -> security.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers(HttpMethod.GET, PUBLIC_PAGES).permitAll()
                        .requestMatchers(HttpMethod.HEAD, PUBLIC_PAGES).permitAll()
                        .requestMatchers(HttpMethod.GET, "/assets/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/assets/**").permitAll()
                        .requestMatchers("/error", "/login").permitAll()
                        .requestMatchers("/admin").hasRole("ADMIN")
                        .requestMatchers("/portaal.html").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .csrf((csrf) -> csrf.csrfTokenRepository(httpOnlyCsrfRepository()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin((form) -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/admin", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout((logout) -> logout.logoutSuccessUrl("/login?logout"))
                .exceptionHandling((exceptions) -> exceptions.authenticationEntryPoint((request, response, authException) -> {
                    String requestUri = request.getRequestURI();
                    String redirectTarget = "/portaal.html".equals(requestUri) ? "/inloggen.html" : "/login";
                    new LoginUrlAuthenticationEntryPoint(redirectTarget).commence(request, response, authException);
                }));

        applySecurityHeaders(http);
        return http.build();
    }

    private static org.springframework.security.web.csrf.CookieCsrfTokenRepository httpOnlyCsrfRepository() {
        var repo = new org.springframework.security.web.csrf.CookieCsrfTokenRepository();
        repo.setCookieCustomizer(cookie -> cookie.httpOnly(true));
        return repo;
    }

    private void applySecurityHeaders(HttpSecurity http) {
        http.headers((headers) -> headers
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
    }
}
