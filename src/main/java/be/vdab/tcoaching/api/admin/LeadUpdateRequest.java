package be.vdab.tcoaching.api.admin;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LeadUpdateRequest(
        @Pattern(regexp = "new|in_progress|completed|archived") @Size(max = 20) String status,
        @Size(max = 4000) String adminNotes,
        Boolean archived
) {
    public LeadUpdateRequest {
        status = normalize(status);
        adminNotes = normalize(adminNotes);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
