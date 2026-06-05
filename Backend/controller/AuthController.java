package com.hotelcraft.controller;

import com.hotelcraft.dto.SignupRequest;
import com.hotelcraft.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        model.addAttribute("signupRequest", new SignupRequest());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String processSignup(@Valid @ModelAttribute("signupRequest") SignupRequest request,
            BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "auth/signup";
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.signupRequest", "Passwords do not match");
            return "auth/signup";
        }

        try {
            userService.registerNewUser(request);
            return "redirect:/login?signupSuccess=true";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auth/signup";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "auth/forgot-password";
    }
}
