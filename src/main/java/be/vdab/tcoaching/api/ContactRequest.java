package be.vdab.tcoaching.api;

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
        name = normalize(name);
        email = normalize(email);
        requestType = normalize(requestType);
        phone = normalize(phone);
        topic = normalize(topic);
        time = normalize(time);
        goal = normalize(goal);
        message = normalize(message);
        page = normalize(page);
        lang = normalize(lang);
        website = normalize(website);
        captchaToken = normalize(captchaToken);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
