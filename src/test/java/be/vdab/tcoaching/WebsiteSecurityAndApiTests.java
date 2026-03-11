package be.vdab.tcoaching;

import be.vdab.tcoaching.api.contact.CaptchaVerificationService;
import be.vdab.tcoaching.api.contact.EmailNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "security.rate-limit.enabled=false",
                "security.admin.username=",
                "security.admin.password=",
                "contact.captcha.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:tcoaching;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.username=sa",
                "spring.datasource.password="
        }
)
class WebsiteSecurityAndApiTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    private MockMvc mockMvc;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CaptchaVerificationService captchaVerificationService;

    @MockitoBean
    private EmailNotificationService emailNotificationService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void publicConfigIsPublicAndHardened() throws Exception {
        mockMvc.perform(get("/api/public-config"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("default-src 'self'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void contactAcceptsValidPayloadWithoutCsrfForPublicForm() throws Exception {
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Thomas Example",
                                  "email": "thomas@example.com",
                                  "requestType": "intake",
                                  "phone": "+32478000000",
                                  "topic": "life",
                                  "time": "Avond",
                                  "goal": "Meer rust",
                                  "message": "Graag intake",
                                  "page": "Home",
                                  "lang": "nl"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(jdbcTemplate, times(1)).update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(emailNotificationService, times(1)).sendContactNotification(any(), any(), any(), any());
    }

    @Test
    void contactRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(jdbcTemplate, emailNotificationService, captchaVerificationService);
    }

    @Test
    void contactRejectsUnknownRequestType() throws Exception {
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Thomas Example",
                                  "email": "thomas@example.com",
                                  "requestType": "vip"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(jdbcTemplate, emailNotificationService, captchaVerificationService);
    }

    @Test
    void trackingAcceptsPublicRequestWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "path": "/index.html",
                                  "title": "Home",
                                  "referrer": "https://example.com",
                                  "lang": "nl"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(jdbcTemplate, times(1)).update(anyString(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void trackingAcceptsAnalyticsEventsWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "path": "/index.html",
                                  "title": "Home",
                                  "referrer": "https://example.com",
                                  "lang": "nl",
                                  "eventType": "cta_click",
                                  "eventName": "hero_intake",
                                  "eventValue": "Plan gratis intake"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(jdbcTemplate, times(1)).update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void adminRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/admin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownProtectedRouteRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/internal"))
                .andExpect(status().isUnauthorized());
    }
}
