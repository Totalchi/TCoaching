package be.vdab.tcoaching.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

    @GetMapping("/admin")
    public String adminDashboard() {
        return "admin";
    }
}
