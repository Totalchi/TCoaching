package be.vdab.tcoaching;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrackingControllerTests extends AbstractWebMvcTest {

    @Test
    void trackingAcceptsPublicRequestWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/track")
                        .with((request) -> {
                            request.setRemoteAddr("203.0.113.42");
                            return request;
                        })
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

        verify(jdbcTemplate, times(1)).update(anyString(), any(), any(), any(), any(), eq("203.0.113.0"), any());
    }

    @Test
    void trackingAcceptsAnalyticsEventsWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/track")
                        .with((request) -> {
                            request.setRemoteAddr("203.0.113.42");
                            return request;
                        })
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

        verify(jdbcTemplate, times(1)).update(anyString(), any(), any(), any(), any(), any(), any(), any(), eq("203.0.113.0"), any());
    }
}
