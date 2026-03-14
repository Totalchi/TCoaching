package be.vdab.tcoaching.api.admin;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardData.DashboardResponse getDashboard() {
        return adminDashboardService.getDashboard();
    }

    @GetMapping("/contacts")
    public AdminDashboardData.ContactLeadPage getContacts(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "40") int limit
    ) {
        return adminDashboardService.getContacts(offset, limit);
    }

    @PatchMapping("/contacts/{id}")
    public void updateContact(
            @PathVariable long id,
            @Valid @RequestBody LeadUpdateRequest request
    ) {
        adminDashboardService.updateContact(id, request);
    }
}
