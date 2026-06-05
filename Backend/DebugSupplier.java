package com.hotelcraft;

import com.hotelcraft.model.User;
import com.hotelcraft.repository.SupplierRepository;
import com.hotelcraft.repository.UserRepository;
import com.hotelcraft.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DebugSupplier implements CommandLineRunner {
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final RoleRepository roleRepository;

    public DebugSupplier(UserRepository userRepository, SupplierRepository supplierRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.supplierRepository = supplierRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        System.out.println("--- DEBUG SUPPLIER DATA ---");
        System.out.println("Available Roles:");
        roleRepository.findAll().forEach(r -> System.out.println("Role: " + r.getName()));
        
        System.out.println("All Users:");
        userRepository.findAll().forEach(u -> {
            System.out.print("User: " + u.getEmail() + " Roles: ");
            u.getRoles().forEach(r -> System.out.print(r.getName() + " "));
            System.out.println();
        });

        System.out.println("All Suppliers:");
        supplierRepository.findAll().forEach(s -> {
            System.out.println("Supplier: " + s.getName() + " (User: " + (s.getUser() != null ? s.getUser().getEmail() : "NULL") + ")");
        });
        System.out.println("--- END DEBUG ---");
    }
}
