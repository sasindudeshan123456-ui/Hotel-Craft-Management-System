package com.hotelcraft.controller;

import com.hotelcraft.model.Permission;
import com.hotelcraft.service.PermissionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/permissions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPermissionController {

    private final PermissionService permissionService;

    public AdminPermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public String listPermissions(Model model) {
        model.addAttribute("permissions", permissionService.findAll());
        model.addAttribute("newPermission", new Permission());
        return "admin/permissions/list";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public String createPermission(@ModelAttribute("newPermission") Permission permission,
            RedirectAttributes redirectAttributes) {
        permissionService.getOrCreate(permission.getName().toUpperCase());
        redirectAttributes.addFlashAttribute("success", "Permission added successfully.");
        return "redirect:/admin/permissions";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public String editPermission(@PathVariable("id") Long id, @RequestParam("name") String name,
            RedirectAttributes redirectAttributes) {
        permissionService.updatePermission(id, name);
        redirectAttributes.addFlashAttribute("success", "Permission updated.");
        return "redirect:/admin/permissions";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public String deletePermission(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        permissionService.deletePermission(id);
        redirectAttributes.addFlashAttribute("success", "Permission deleted.");
        return "redirect:/admin/permissions";
    }
}
