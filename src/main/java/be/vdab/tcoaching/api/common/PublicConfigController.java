package be.vdab.tcoaching.api.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PublicConfigController {
    private final boolean captchaEnabled;
    private final String captchaSiteKey;
    private final String bookingUrl;

    public PublicConfigController(
            @Value("${contact.captcha.enabled:true}") boolean captchaEnabled,
            @Value("${contact.captcha.site-key:}") String captchaSiteKey,
            @Value("${contact.booking.url:}") String bookingUrl
    ) {
        this.captchaEnabled = captchaEnabled;
        this.captchaSiteKey = captchaSiteKey;
        this.bookingUrl = bookingUrl;
    }

    @GetMapping("/public-config")
    public PublicConfig publicConfig() {
        String siteKey = hasText(captchaSiteKey) ? captchaSiteKey : null;
        String configuredBookingUrl = hasText(bookingUrl) ? bookingUrl : null;
        return new PublicConfig(captchaEnabled, siteKey, configuredBookingUrl);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record PublicConfig(boolean captchaEnabled, String captchaSiteKey, String bookingUrl) {
    }
}
