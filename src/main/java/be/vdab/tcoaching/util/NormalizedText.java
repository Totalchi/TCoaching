package be.vdab.tcoaching.util;

public final class NormalizedText {
    private NormalizedText() {
    }

    public static String normalizeToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
