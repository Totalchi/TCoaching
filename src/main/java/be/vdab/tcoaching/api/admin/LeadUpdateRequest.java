package be.vdab.tcoaching.api.admin;

import be.vdab.tcoaching.util.NormalizedText;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LeadUpdateRequest(
        @Pattern(regexp = "new|in_progress|completed|archived") @Size(max = 20) String status,
        @Size(max = 4000) String adminNotes,
        Boolean archived
) {
    public LeadUpdateRequest {
        status = NormalizedText.normalizeToNull(status);
        adminNotes = NormalizedText.normalizeToNull(adminNotes);
    }
}
