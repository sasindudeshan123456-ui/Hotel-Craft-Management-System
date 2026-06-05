package com.hotelcraft.config;

import com.hotelcraft.model.*;
import com.hotelcraft.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductRepository productRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final MaterialCategoryRepository categoryRepository;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final PaymentRepository paymentRepository;

    public DataInitializer(UserRepository userRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            PasswordEncoder passwordEncoder,
            ProductRepository productRepository,
            RawMaterialRepository rawMaterialRepository,
            MaterialCategoryRepository categoryRepository,
            BookingRepository bookingRepository,
            BookingItemRepository bookingItemRepository,
            PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.productRepository = productRepository;
        this.rawMaterialRepository = rawMaterialRepository;
        this.categoryRepository = categoryRepository;
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Data wipe disabled for persistence
        // --- Seed Permissions ---
        Permission userRead = perm("USER_READ");
        Permission userCreate = perm("USER_CREATE");
        Permission userUpdate = perm("USER_UPDATE");
        Permission userDelete = perm("USER_DELETE");
        Permission roleRead = perm("ROLE_READ");
        Permission roleCreate = perm("ROLE_CREATE");
        Permission roleUpdate = perm("ROLE_UPDATE");
        Permission roleDelete = perm("ROLE_DELETE");

        // --- Inventory Permissions ---
        Permission invRead = perm("INVENTORY_READ");
        Permission invReadAll = perm("INVENTORY_READ_ALL");
        Permission invCreate = perm("INVENTORY_CREATE");
        Permission invUpdate = perm("INVENTORY_UPDATE");
        Permission invDelete = perm("INVENTORY_DELETE");

        // --- Booking Permissions ---
        Permission bookingRead = perm("BOOKING_READ_ALL");
        Permission bookingManage = perm("BOOKING_MANAGE");

        // --- Report Permissions ---
        Permission reportRead = perm("REPORT_READ");

        // --- Cart Permissions ---
        Permission cartEdit = perm("CART_EDIT");

        // --- Supplier Permissions ---
        Permission supplierAccess = perm("SUPPLIER_ACCESS");
        Permission supplierOffer = perm("SUPPLIER_OFFER_CREATE");
        Permission supplierContract = perm("SUPPLIER_CONTRACT_VIEW");

        // ADMIN gets EVERYTHING
        Role adminRole = seedRole("ROLE_ADMIN",
                Set.of(userRead, userCreate, userUpdate, userDelete,
                        roleRead, roleCreate, roleUpdate, roleDelete,
                        invRead, invReadAll, invCreate, invUpdate, invDelete,
                        bookingRead, bookingManage, reportRead, cartEdit,
                        supplierAccess, supplierOffer, supplierContract));

        // --- Role Assignments ---
        seedRole("ROLE_SUPPLIER", Set.of(bookingManage, invRead, supplierAccess, supplierOffer, supplierContract));
        seedRole("ROLE_NEW_SUPPLIER", Set.of(bookingManage, invRead, supplierAccess, supplierOffer, supplierContract)); 
        seedRole("ROLE_USER", Set.of(bookingManage, invRead, supplierAccess));

        // --- Seed Admin User ---
        User admin = userRepository.findByEmail("admin@hotelcraft.com").orElse(null);
        if (admin == null) {
            admin = new User();
            admin.setFullName("System Admin");
            admin.setEmail("admin@hotelcraft.com");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setPhone("1234567890");
            admin.setAddress("System HQ");
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);
        } else if (admin.getRoles().isEmpty() || !admin.getRoles().contains(adminRole)) {
            admin.getRoles().add(adminRole);
            userRepository.save(admin);
        }

        // --- Cleanup Dummy Values ---
        Set<String> dummyRoles = Set.of(
                "ROLE_BOOKING_MANAGER", "ROLE_CART_EDIT", "ROLE_INVENTORY_MANAGER",
                "ROLE_STORE_ADMIN", "ROLE_STORE_CREATE",
                "ROLE_PROJECT_MANAGER", "ROLE_FINANCE_MANAGER", "ROLE_USER_MANAGER");

        roleRepository.findAll().forEach(role -> {
            if (dummyRoles.contains(role.getName())) {
                // Remove role from all users first to avoid constraint violations
                userRepository.findAll().forEach(user -> {
                    if (user.getRoles().contains(role)) {
                        user.getRoles().remove(role);
                        userRepository.save(user);
                    }
                });
                roleRepository.delete(role);
            }
        });

        seedProducts();
    }

    private void seedProducts() {
        // Garment Items
        seedProduct("Satin Chair Covers", "Garments", 15.0, 100, "Elegant white satin chair covers", "/images/products/satin_chair_cover.png");
        seedProduct("Damask Table Covers", "Garments", 45.0, 50, "Intricate damask patterns", "/images/products/damask_table_cover.png");
        seedProduct("Egyptian Cotton Bed Sheets", "Garments", 85.0, 200, "Luxury hotel bedding", "/images/products/egyptian_sheets.png");

        // Iron Items
        seedProduct("Wrought Iron Buffet Stand", "Ironware", 120.0, 10, "Modern industrial buffet set", null);
        
        // Remove unwanted items if they exist
        removeProduct("Modern Steel Hotel Chair");
        removeProduct("Heavy-Duty Banquet Table");
    }

    private void removeProduct(String name) {
        productRepository.findByNameIgnoreCase(name).ifPresent(productRepository::delete);
    }

    private void seedProduct(String name, String cat, Double price, Integer qty, String desc, String img) {
        Optional<Product> existing = productRepository.findByNameIgnoreCase(name);
        if (existing.isEmpty()) {
            Product p = new Product(name, cat, price, qty, desc, "ACTIVE");
            p.setImagePath(img);
            productRepository.save(p);
        } else {
            Product p = existing.get();
            p.setCategory(cat);
            if (img != null) p.setImagePath(img);
            productRepository.save(p);
        }
    }

    private Permission perm(String name) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(new Permission(name)));
    }

    private Role seedRole(String name, Set<Permission> permissions) {
        Role role = roleRepository.findByName(name).orElse(new Role(name));
        role.setPermissions(new HashSet<>(permissions));
        return roleRepository.save(role);
    }
}
