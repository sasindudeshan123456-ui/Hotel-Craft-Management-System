package com.hotelcraft.controller;

import com.hotelcraft.dto.RoleDto;
import com.hotelcraft.service.PermissionService;
import com.hotelcraft.service.RoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;

    public AdminRoleController(RoleService roleService, PermissionService permissionService) {
        this.roleService = roleService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public String listRoles(Model model) {
        model.addAttribute("roles", roleService.findAll());
        return "admin/roles/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public String newRoleForm(Model model) {
        model.addAttribute("roleDto", new RoleDto());
        model.addAttribute("allPermissions", permissionService.findAll());
        model.addAttribute("editMode", false);
        return "admin/roles/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public String createRole(@ModelAttribute("roleDto") RoleDto dto,
            RedirectAttributes redirectAttributes) {
        roleService.save(dto);
        redirectAttributes.addFlashAttribute("success", "Role created successfully.");
        return "redirect:/admin/roles";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public String editRoleForm(@PathVariable("id") Long id, Model model) {
        var role = roleService.findById(id);
        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setPermissionIds(role.getPermissions().stream().map(p -> p.getId()).toList());
        model.addAttribute("roleDto", dto);
        model.addAttribute("allPermissions", permissionService.findAll());
        model.addAttribute("editMode", true);
        return "admin/roles/form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public String updateRole(@PathVariable("id") Long id,
            @ModelAttribute("roleDto") RoleDto dto,
            RedirectAttributes redirectAttributes) {
        dto.setId(id);
        roleService.save(dto);
        redirectAttributes.addFlashAttribute("success", "Role updated successfully.");
        return "redirect:/admin/roles";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public String deleteRole(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        roleService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Role deleted.");
        return "redirect:/admin/roles";
    }
}
