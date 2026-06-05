package com.hotelcraft;

import com.hotelcraft.repository.SupplierRepository;
import com.hotelcraft.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DbCheck implements CommandLineRunner {
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;

    public DbCheck(UserRepository userRepository, SupplierRepository supplierRepository) {
        this.userRepository = userRepository;
        this.supplierRepository = supplierRepository;
    }

    @Override
    public void run(String... args) {
        System.out.println("USER COUNT: " + userRepository.count());
        System.out.println("SUPPLIER COUNT: " + supplierRepository.count());
        userRepository.findAll().forEach(u -> System.out.println("User: " + u.getEmail()));
        supplierRepository.findAll().forEach(s -> System.out.println("Supplier: " + s.getName() + " (User: " + (s.getUser() != null ? s.getUser().getEmail() : "NULL") + ")"));
    }
}
