package be.vdab.tcoaching.api.tracking;

import be.vdab.tcoaching.util.NormalizedText;
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
        path = NormalizedText.normalizeToNull(path);
        title = NormalizedText.normalizeToNull(title);
        referrer = NormalizedText.normalizeToNull(referrer);
        lang = NormalizedText.normalizeToNull(lang);
        eventType = NormalizedText.normalizeToNull(eventType);
        eventName = NormalizedText.normalizeToNull(eventName);
        eventValue = NormalizedText.normalizeToNull(eventValue);
    }

    public boolean isPageView() {
        return eventType == null && eventName == null && eventValue == null;
    }
}
