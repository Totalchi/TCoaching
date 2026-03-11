package be.vdab.tcoaching.api.tracking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PageViewRequest(
        @NotBlank @Size(max = 200) String path,
        @Size(max = 200) String title,
        @Size(max = 500) String referrer,
        @Pattern(regexp = "nl|en") @Size(max = 10) String lang,
        @Size(max = 40) String eventType,
        @Size(max = 80) String eventName,
        @Size(max = 255) String eventValue
) {
    public PageViewRequest {
        path = normalize(path);
        title = normalize(title);
        referrer = normalize(referrer);
        lang = normalize(lang);
        eventType = normalize(eventType);
        eventName = normalize(eventName);
        eventValue = normalize(eventValue);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public boolean isPageView() {
        return eventType == null && eventName == null && eventValue == null;
    }
}
