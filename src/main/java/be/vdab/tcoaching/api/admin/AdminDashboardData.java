package be.vdab.tcoaching.api.admin;

import java.time.Instant;
import java.util.List;

public final class AdminDashboardData {
    private AdminDashboardData() {
    }

    public record DashboardResponse(
            Instant generatedAt,
            Overview overview,
            List<RecentContact> recentContacts,
            List<PageSummary> topPages,
            List<EventSummary> topEvents
    ) {
    }

    public record Overview(
            long totalLeads,
            long leadsLast30Days,
            long pageViewsLast30Days,
            long eventsLast30Days,
            long ctaClicksLast30Days
    ) {
    }

    public record RecentContact(
            String name,
            String email,
            String status,
            String requestType,
            String topic,
            String page,
            Instant createdAt
    ) {
    }

    public record ContactLead(
            long id,
            String name,
            String email,
            String status,
            String requestType,
            String topic,
            String page,
            String phone,
            String preferredTime,
            String goal,
            String message,
            String adminNotes,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ContactLeadPage(
            List<ContactLead> items,
            long total,
            int offset,
            int limit,
            boolean hasMore
    ) {
    }

    public record PageSummary(
            String path,
            long visits,
            Instant lastSeen
    ) {
    }

    public record EventSummary(
            String eventType,
            String eventName,
            long total,
            Instant lastSeen
    ) {
    }
}
