package be.vdab.tcoaching.api.client;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class ClientVerificationController {
    private final ClientPortalService clientPortalService;

    public ClientVerificationController(ClientPortalService clientPortalService) {
        this.clientPortalService = clientPortalService;
    }

    @GetMapping("/api/client/verify-email")
    public String verifyEmail(@RequestParam("token") String token) {
        boolean verified = clientPortalService.verifyEmail(token);
        return verified
                ? "redirect:/inloggen.html?verified=1"
                : "redirect:/inloggen.html?verification=invalid";
    }
}
