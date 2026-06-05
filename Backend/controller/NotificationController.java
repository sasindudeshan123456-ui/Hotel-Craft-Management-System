package com.hotelcraft.controller;

import com.hotelcraft.model.Notification;
import com.hotelcraft.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String showForm(Model model) {
        model.addAttribute("notification", new Notification());
        return "admin/notifications/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String createNotification(@ModelAttribute Notification notification, RedirectAttributes redirectAttributes) {
        notificationService.createNotification(notification.getTitle(), notification.getMessage(), notification.getType());
        redirectAttributes.addFlashAttribute("success", "Notification sent successfully!");
        return "redirect:/dashboard";
    }

    @GetMapping("/api/count")
    @ResponseBody
    public long getCount() {
        return notificationService.getNotificationCount();
    }

    @GetMapping("/api/list")
    @ResponseBody
    public List<Notification> getNotifications() {
        return notificationService.getAllNotifications();
    }
}
