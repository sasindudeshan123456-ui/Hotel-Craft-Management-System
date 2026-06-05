package com.hotelcraft.service;

import com.hotelcraft.dto.SignupRequest;
import com.hotelcraft.model.Supplier;
import com.hotelcraft.model.User;
import com.hotelcraft.model.Role;
import com.hotelcraft.repository.RoleRepository;
import com.hotelcraft.repository.SupplierRepository;
import com.hotelcraft.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SupplierService {

    private final UserService userService;
    private final SupplierRepository supplierRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public SupplierService(UserService userService, SupplierRepository supplierRepository, RoleRepository roleRepository, UserRepository userRepository) {
        this.userService = userService;
        this.supplierRepository = supplierRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    public Supplier registerSupplier(Supplier supplier, String email, String password, String phone, String address) {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setFullName(supplier.getName());
        signupRequest.setEmail(email);
        signupRequest.setPassword(password);
        signupRequest.setConfirmPassword(password);
        signupRequest.setPhone(phone);
        signupRequest.setAddress(address);

        // Check if user already exists
        User user = userService.findByEmail(email).orElse(null);
        if (user == null) {
            user = userService.registerNewUser(signupRequest, "ROLE_NEW_SUPPLIER");
        } else {
            // If user exists, check if they are already a supplier
            if (supplierRepository.findByUser(user).isPresent()) {
                throw new IllegalArgumentException("This email is already associated with a supplier account.");
            }
            // Ensure they have the ROLE_NEW_SUPPLIER
            if (user.getRoles().stream().noneMatch(r -> r.getName().equals("ROLE_NEW_SUPPLIER") || r.getName().equals("ROLE_SUPPLIER"))) {
                Role newSupplierRole = roleRepository.findByName("ROLE_NEW_SUPPLIER").orElseThrow(() -> new IllegalStateException("ROLE_NEW_SUPPLIER not found"));
                user.getRoles().add(newSupplierRole);
                userRepository.save(user);
            }
        }

        supplier.setUser(user);
        
        // Generate initials if not provided
        if (supplier.getInitials() == null || supplier.getInitials().isEmpty()) {
            String[] names = supplier.getName().split(" ");
            String initials = String.valueOf(names[0].charAt(0));
            if (names.length > 1) initials += names[1].charAt(0);
            supplier.setInitials(initials.toUpperCase());
        }

        if (supplier.getStatus() == null) {
            supplier.setStatus("REVIEW");
        }
        
        supplier.setOnTimeRate(0.0);
        supplier.setOrderCount(0);
        supplier.setRating(0.0);

        return supplierRepository.save(supplier);
    }

    public void activateSupplier(Supplier supplier) {
        supplier.setStatus("ACTIVE");
        User user = supplier.getUser();
        if (user != null) {
            // Remove ROLE_NEW_SUPPLIER if it exists
            user.getRoles().removeIf(r -> r.getName().equals("ROLE_NEW_SUPPLIER"));
            // Add ROLE_SUPPLIER if it doesn't exist
            if (user.getRoles().stream().noneMatch(r -> r.getName().equals("ROLE_SUPPLIER"))) {
                Role supplierRole = roleRepository.findByName("ROLE_SUPPLIER")
                    .orElseThrow(() -> new IllegalStateException("ROLE_SUPPLIER not found"));
                user.getRoles().add(supplierRole);
            }
            userRepository.save(user);
        }
        supplierRepository.save(supplier);
    }

    public void deleteSupplierAccount(User user) {
        Supplier supplier = supplierRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("No supplier account found for this user."));
        
        // 1. Remove the supplier roles from the user
        user.getRoles().removeIf(role -> role.getName().equals("ROLE_SUPPLIER") || role.getName().equals("ROLE_NEW_SUPPLIER"));
        user.setSupplier(null);
        userRepository.save(user);

        // 2. Delete the supplier record (contracts and orders will be cascaded)
        supplierRepository.delete(supplier);
    }
}
