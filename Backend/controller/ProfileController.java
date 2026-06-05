package com.hotelcraft.controller;

import com.hotelcraft.dto.ChangePasswordRequest;
import com.hotelcraft.dto.UpdateProfileRequest;
import com.hotelcraft.model.User;
import com.hotelcraft.security.CustomUserDetails;
import com.hotelcraft.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        model.addAttribute("user", user);

        UpdateProfileRequest updateRequest = new UpdateProfileRequest();
        updateRequest.setFullName(user.getFullName());
        updateRequest.setEmail(user.getEmail());
        updateRequest.setPhone(user.getPhone());
        updateRequest.setAddress(user.getAddress());
        model.addAttribute("updateRequest", updateRequest);

        model.addAttribute("passwordRequest", new ChangePasswordRequest());
        return "profile";
    }

    @PostMapping("/update")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute("updateRequest") UpdateProfileRequest req,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (result.hasErrors()) {
            model.addAttribute("user", userDetails.getUser());
            // Do NOT overwrite updateRequest, Spring keeps the erroneous one in the model
            model.addAttribute("passwordRequest", new ChangePasswordRequest());
            return "profile";
        }
        try {
            userService.updateProfile(userDetails.getUser(), req);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute("passwordRequest") ChangePasswordRequest req,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (result.hasErrors()) {
            User user = userDetails.getUser();
            model.addAttribute("user", user);
            
            // Collect existing profile info but don't overwrite the validated passwordRequest
            UpdateProfileRequest updateRequest = new UpdateProfileRequest();
            updateRequest.setFullName(user.getFullName());
            updateRequest.setEmail(user.getEmail());
            updateRequest.setPhone(user.getPhone());
            updateRequest.setAddress(user.getAddress());
            model.addAttribute("updateRequest", updateRequest);
            
            return "profile";
        }
        try {
            userService.changePassword(userDetails.getUser(), req);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/delete-account")
    public String deleteAccount(@AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            request.getSession().invalidate();
            userService.deleteUser(userDetails.getUser().getId());
            redirectAttributes.addFlashAttribute("success", "Account deleted.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not delete account.");
            return "redirect:/profile";
        }
    }
}
