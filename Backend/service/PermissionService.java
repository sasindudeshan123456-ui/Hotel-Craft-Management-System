package com.hotelcraft.service;

import com.hotelcraft.model.Permission;
import com.hotelcraft.repository.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final com.hotelcraft.repository.RoleRepository roleRepository;

    public PermissionService(PermissionRepository permissionRepository,
            com.hotelcraft.repository.RoleRepository roleRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
    }

    public Permission getOrCreate(String name) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(new Permission(name)));
    }

    public List<Permission> findAll() {
        return permissionRepository.findAll();
    }

    public Permission findById(Long id) {
        return permissionRepository.findById(id).orElseThrow();
    }

    public void updatePermission(Long id, String newName) {
        Permission permission = findById(id);
        permission.setName(newName.toUpperCase());
        permissionRepository.save(permission);
    }

    public void deletePermission(Long id) {
        Permission permission = findById(id);

        // Remove this permission from all roles that have it
        List<com.hotelcraft.model.Role> roles = roleRepository.findAll();
        for (com.hotelcraft.model.Role role : roles) {
            if (role.getPermissions().remove(permission)) {
                roleRepository.save(role);
            }
        }

        permissionRepository.delete(permission);
    }
}
