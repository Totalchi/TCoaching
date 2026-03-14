package be.vdab.tcoaching;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContactControllerTests extends AbstractWebMvcTest {

    @Test
    void contactAcceptsValidPayloadWithoutCsrfForPublicForm() throws Exception {
        AtomicBoolean transactionActiveDuringInsert = new AtomicBoolean(false);
        doAnswer((ignoredInvocation) -> {
            transactionActiveDuringInsert.set(TransactionSynchronizationManager.isActualTransactionActive());
            return 1;
        }).when(jdbcTemplate).update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

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

        org.junit.jupiter.api.Assertions.assertTrue(transactionActiveDuringInsert.get());
        verify(jdbcTemplate, times(1)).update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(emailNotificationService, times(1)).sendContactNotification(any(), any(), any(), any());
    }

    @Test
    void contactEndpointRemainsTransactional() throws Exception {
        Method submitContact = be.vdab.tcoaching.api.contact.ContactController.class.getMethod(
                "submitContact",
                be.vdab.tcoaching.api.contact.ContactRequest.class,
                jakarta.servlet.http.HttpServletRequest.class
        );

        org.junit.jupiter.api.Assertions.assertNotNull(
                submitContact.getAnnotation(org.springframework.transaction.annotation.Transactional.class)
        );
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
