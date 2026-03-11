package be.vdab.tcoaching;

import be.vdab.tcoaching.api.contact.CaptchaVerificationService;
import be.vdab.tcoaching.api.contact.EmailNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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
abstract class AbstractWebMvcTest {

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    protected MockMvc mockMvc;

    @MockitoBean
    protected JdbcTemplate jdbcTemplate;

    @MockitoBean
    protected CaptchaVerificationService captchaVerificationService;

    @MockitoBean
    protected EmailNotificationService emailNotificationService;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }
}
