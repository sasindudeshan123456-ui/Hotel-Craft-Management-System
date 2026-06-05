package com.hotelcraft.service;

import com.hotelcraft.dto.RoleDto;
import com.hotelcraft.model.Permission;
import com.hotelcraft.model.Role;
import com.hotelcraft.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionService permissionService;

    public RoleService(RoleRepository roleRepository, PermissionService permissionService) {
        this.roleRepository = roleRepository;
        this.permissionService = permissionService;
    }

    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    public Role findById(Long id) {
        return roleRepository.findById(id).orElseThrow();
    }

    public Role findByName(String name) {
        return roleRepository.findByName(name).orElseThrow();
    }

    public Role save(RoleDto dto) {
        Role role = dto.getId() != null ? findById(dto.getId()) : new Role();
        role.setName(dto.getName());

        Set<Permission> perms = new HashSet<>();
        if (dto.getPermissionIds() != null) {
            for (Long pid : dto.getPermissionIds()) {
                perms.add(permissionService.findById(pid));
            }
        }
        role.setPermissions(perms);
        return roleRepository.save(role);
    }

    public void delete(Long id) {
        roleRepository.deleteById(id);
    }
}

