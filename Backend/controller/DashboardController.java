package com.hotelcraft.controller;

import com.hotelcraft.model.User;
import com.hotelcraft.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.security.Principal;

@Controller
public class DashboardController {

    @Autowired
    private UserService userService;

    @GetMapping({ "/", "/dashboard" })
    public String dashboard(Principal principal) {
        if (principal != null) {
            User user = userService.findByEmail(principal.getName()).orElse(null);
            if (user != null && user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_SUPPLIER"))) {
                return "redirect:/suppliers/portal";
            }
        }
        return "dashboard";
    }
}
