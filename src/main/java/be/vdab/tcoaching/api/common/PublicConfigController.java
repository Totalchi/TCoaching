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
    private final String contactEmail;
    private final String contactPhoneDisplay;
    private final String contactPhoneHref;

    public PublicConfigController(
            @Value("${contact.captcha.enabled:true}") boolean captchaEnabled,
            @Value("${contact.captcha.site-key:}") String captchaSiteKey,
            @Value("${contact.booking.url:}") String bookingUrl,
            @Value("${contact.public.email:}") String contactEmail,
            @Value("${contact.public.phone-display:}") String contactPhoneDisplay,
            @Value("${contact.public.phone-link:}") String contactPhoneHref
    ) {
        this.captchaEnabled = captchaEnabled;
        this.captchaSiteKey = captchaSiteKey;
        this.bookingUrl = bookingUrl;
        this.contactEmail = contactEmail;
        this.contactPhoneDisplay = contactPhoneDisplay;
        this.contactPhoneHref = contactPhoneHref;
    }

    @GetMapping("/public-config")
    public PublicConfig publicConfig() {
        String siteKey = hasText(captchaSiteKey) ? captchaSiteKey : null;
        String configuredBookingUrl = hasText(bookingUrl) ? bookingUrl : null;
        String configuredContactEmail = hasText(contactEmail) ? contactEmail.trim() : null;
        String configuredContactPhoneDisplay = hasText(contactPhoneDisplay) ? contactPhoneDisplay.trim() : null;
        String configuredContactPhoneHref = hasText(contactPhoneHref) ? contactPhoneHref.trim() : null;
        return new PublicConfig(
                captchaEnabled,
                siteKey,
                configuredBookingUrl,
                configuredContactEmail,
                configuredContactPhoneDisplay,
                configuredContactPhoneHref
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record PublicConfig(
            boolean captchaEnabled,
            String captchaSiteKey,
            String bookingUrl,
            String contactEmail,
            String contactPhoneDisplay,
            String contactPhoneHref
    ) {
    }
}
