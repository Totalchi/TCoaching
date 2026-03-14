package be.vdab.tcoaching.api.contact;

import be.vdab.tcoaching.util.NormalizedText;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Email @Size(max = 120) String email,
        @Pattern(regexp = "intake|waitlist|lead") @Size(max = 20) String requestType,
        @Size(max = 40) String phone,
        @Pattern(regexp = "life|pt|stress|assertive|other|lead-magnet") @Size(max = 20) String topic,
        @Size(max = 80) String time,
        @Size(max = 1000) String goal,
        @Size(max = 1000) String message,
        @Size(max = 60) String page,
        @Pattern(regexp = "nl|en") @Size(max = 10) String lang,
        @Size(max = 80) String website,
        @Size(max = 2048) String captchaToken
) {
    public ContactRequest {
        name = NormalizedText.normalizeToNull(name);
        email = NormalizedText.normalizeToNull(email);
        requestType = NormalizedText.normalizeToNull(requestType);
        phone = NormalizedText.normalizeToNull(phone);
        topic = NormalizedText.normalizeToNull(topic);
        time = NormalizedText.normalizeToNull(time);
        goal = NormalizedText.normalizeToNull(goal);
        message = NormalizedText.normalizeToNull(message);
        page = NormalizedText.normalizeToNull(page);
        lang = NormalizedText.normalizeToNull(lang);
        website = NormalizedText.normalizeToNull(website);
        captchaToken = NormalizedText.normalizeToNull(captchaToken);
    }
}
