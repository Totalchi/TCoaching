package be.vdab.tcoaching;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContactControllerTests extends AbstractWebMvcTest {

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
}
