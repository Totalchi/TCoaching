package be.vdab.tcoaching.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PublicConfigController {
    private final boolean captchaEnabled;
    private final String captchaSiteKey;

    public PublicConfigController(
            @Value("${contact.captcha.enabled:true}") boolean captchaEnabled,
            @Value("${contact.captcha.site-key:}") String captchaSiteKey
    ) {
        this.captchaEnabled = captchaEnabled;
        this.captchaSiteKey = captchaSiteKey;
    }

    @GetMapping("/public-config")
    public PublicConfig publicConfig() {
        String siteKey = hasText(captchaSiteKey) ? captchaSiteKey : null;
        return new PublicConfig(captchaEnabled, siteKey);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record PublicConfig(boolean captchaEnabled, String captchaSiteKey) {
    }
}
