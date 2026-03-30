package be.vdab.tcoaching.api.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

record ClientRegistrationRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$") String password,
        @NotBlank @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 30) String phone,
        @Pattern(regexp = "nl|en") String lang
) {
}

record ClientLoginRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}

record ClientResetPasswordRequest(
        @NotBlank @Email @Size(max = 255) String email
) {
}

record ClientResetPasswordConfirmRequest(
        @NotBlank @Pattern(regexp = "[a-f0-9]{64}") String token,
        @NotBlank @Size(min = 8, max = 72) @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$") String password
) {
}

record ClientMessageRequest(
        @NotBlank @Size(min = 2, max = 2000) String body
) {
}

record ClientTrainingItemUpdateRequest(boolean completed) {
}

record ClientAuthResponse(
        long clientId,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String lang
) {
}

record ClientDashboardResponse(
        String fullName,
        String lang,
        NextAppointmentResponse nextAppointment,
        int openInvoiceCount,
        int unreadCoachMessages,
        String activeTrainingPlanTitle,
        int activeTrainingItemCount,
        MessageResponse latestMessage
) {
}

record NextAppointmentResponse(
        long id,
        String title,
        String type,
        LocalDateTime scheduledAt,
        int durationMin,
        String location,
        String status,
        String notesShared
) {
}

record AppointmentResponse(
        long id,
        String title,
        String type,
        LocalDateTime scheduledAt,
        int durationMin,
        String location,
        String status,
        String notesShared
) {
}

record TrainingPlanResponse(
        long id,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        List<TrainingItemResponse> items
) {
}

record TrainingItemResponse(
        long id,
        int sortOrder,
        String category,
        String title,
        String description,
        Integer sets,
        Integer reps,
        Integer durationSec,
        boolean completedByClient
) {
}

record InvoiceResponse(
        long id,
        String invoiceNumber,
        String description,
        int amountCents,
        String currency,
        String status,
        LocalDate dueDate,
        LocalDateTime paidAt,
        String paymentMethod,
        String pdfUrl
) {
}

record MessageResponse(
        long id,
        String sender,
        String body,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
}

enum ClientTokenType {
    VERIFY_EMAIL("verify_email"),
    RESET_PASSWORD("reset_password");

    private final String databaseValue;

    ClientTokenType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    String databaseValue() {
        return databaseValue;
    }
}
