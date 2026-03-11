package be.vdab.tcoaching.api.contact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CaptchaVerificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaptchaVerificationService.class);

    private final RestClient restClient;
    private final boolean enabled;
    private final String secret;
    private final String verifyUrl;

    public CaptchaVerificationService(
            @Value("${contact.captcha.enabled:true}") boolean enabled,
            @Value("${contact.captcha.secret:}") String secret,
            @Value("${contact.captcha.verify-url:https://challenges.cloudflare.com/turnstile/v0/siteverify}") String verifyUrl
    ) {
        this.restClient = RestClient.builder().build();
        this.enabled = enabled;
        this.secret = secret;
        this.verifyUrl = verifyUrl;
    }

    public void verify(String token, String remoteIp) {
        if (!enabled) {
            return;
        }
        if (secret == null || secret.isBlank()) {
            LOGGER.warn("Captcha is enabled but no secret is configured.");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secret);
        form.add("response", token);
        if (remoteIp != null && !remoteIp.isBlank()) {
            form.add("remoteip", remoteIp);
        }

        try {
            CaptchaResponse response = restClient
                    .post()
                    .uri(verifyUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(CaptchaResponse.class);

            if (response == null || !response.isSuccess()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        } catch (RestClientException ex) {
            LOGGER.warn("Captcha verification failed: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private record CaptchaResponse(Boolean success) {
        public boolean isSuccess() {
            return Boolean.TRUE.equals(success);
        }
    }
}
