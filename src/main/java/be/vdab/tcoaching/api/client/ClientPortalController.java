package be.vdab.tcoaching.api.client;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/client")
class ClientPortalController {
    private final ClientPortalService clientPortalService;

    public ClientPortalController(ClientPortalService clientPortalService) {
        this.clientPortalService = clientPortalService;
    }

    @GetMapping("/dashboard")
    public ClientDashboardResponse dashboard(@AuthenticationPrincipal ClientPrincipal principal) {
        return clientPortalService.getDashboard(principal);
    }

    @GetMapping("/appointments")
    public List<AppointmentResponse> appointments(@AuthenticationPrincipal ClientPrincipal principal) {
        return clientPortalService.getAppointments(principal.getClientId());
    }

    @GetMapping("/training-plan")
    public TrainingPlanResponse trainingPlan(@AuthenticationPrincipal ClientPrincipal principal) {
        return clientPortalService.getTrainingPlan(principal.getClientId());
    }

    @PatchMapping("/training-items/{itemId}")
    public ResponseEntity<Void> updateTrainingItem(
            @AuthenticationPrincipal ClientPrincipal principal,
            @PathVariable long itemId,
            @RequestBody ClientTrainingItemUpdateRequest request
    ) {
        clientPortalService.updateTrainingItemCompletion(principal.getClientId(), itemId, request.completed());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/invoices")
    public List<InvoiceResponse> invoices(@AuthenticationPrincipal ClientPrincipal principal) {
        return clientPortalService.getInvoices(principal.getClientId());
    }

    @GetMapping("/messages")
    public List<MessageResponse> messages(@AuthenticationPrincipal ClientPrincipal principal) {
        return clientPortalService.getMessages(principal.getClientId());
    }

    @PostMapping("/messages")
    public ResponseEntity<MessageResponse> addMessage(
            @AuthenticationPrincipal ClientPrincipal principal,
            @Valid @RequestBody ClientMessageRequest request
    ) {
        return ResponseEntity.ok(clientPortalService.addClientMessage(principal.getClientId(), request));
    }
}
