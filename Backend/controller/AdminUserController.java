package com.hotelcraft.controller;

import com.hotelcraft.dto.UserDto;
import com.hotelcraft.model.User;
import com.hotelcraft.service.RoleService;
import com.hotelcraft.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasAuthority('USER_READ')")
public class AdminUserController {

    private final UserService userService;
    private final RoleService roleService;

    public AdminUserController(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public String newUserForm(Model model) {
        model.addAttribute("userDto", new UserDto());
        model.addAttribute("roles", roleService.findAll());
        model.addAttribute("editMode", false);
        return "admin/users/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public String createUser(@Valid @ModelAttribute("userDto") UserDto dto,
            BindingResult result, Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("roles", roleService.findAll());
            model.addAttribute("editMode", false);
            return "admin/users/form";
        }
        userService.saveAdminUser(dto);
        redirectAttributes.addFlashAttribute("success", "User created successfully.");
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public String editUserForm(@PathVariable("id") Long id, Model model) {
        User user = userService.findById(id);
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setRoleIds(user.getRoles().stream().map(r -> r.getId()).toList());
        model.addAttribute("userDto", dto);
        model.addAttribute("roles", roleService.findAll());
        model.addAttribute("editMode", true);
        return "admin/users/form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public String updateUser(@PathVariable("id") Long id,
            @Valid @ModelAttribute("userDto") UserDto dto,
            BindingResult result, Model model,
            RedirectAttributes redirectAttributes) {
        dto.setId(id);
        if (result.hasErrors()) {
            model.addAttribute("roles", roleService.findAll());
            model.addAttribute("editMode", true);
            return "admin/users/form";
        }
        userService.saveAdminUser(dto);
        redirectAttributes.addFlashAttribute("success", "User updated successfully.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("success", "User deleted.");
        return "redirect:/admin/users";
    }
}
