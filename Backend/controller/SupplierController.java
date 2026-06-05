package com.hotelcraft.controller;

import com.hotelcraft.model.*;
import com.hotelcraft.dto.SignupRequest;
import com.hotelcraft.service.UserService;
import com.hotelcraft.service.SupplierService;
import com.hotelcraft.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.annotation.PostConstruct;

import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private com.hotelcraft.repository.MaterialCategoryRepository categoryRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private SupplierOrderRepository supplierOrderRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private RawMaterialRepository rawMaterialRepository;

    @Autowired
    private SupplyRequestRepository supplyRequestRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private com.hotelcraft.service.InventoryService inventoryService;

    @Autowired
    private com.hotelcraft.security.CustomUserDetailsService customUserDetailsService;

    @PostConstruct
    public void initData() {
        // Ensure ROLE_SUPPLIER exists
        if (roleRepository.findByName("ROLE_SUPPLIER").isEmpty()) {
            roleRepository.save(new Role("ROLE_SUPPLIER"));
        }
    }

    @GetMapping
    public String listSuppliers(Model model, Principal principal) {
        if (principal == null) {
            model.addAttribute("activePage", "suppliers");
            return "suppliers/welcome";
        }
        
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        
        // If user is a supplier AND active, take them to their dashboard
        if (user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_SUPPLIER"))) {
            Optional<Supplier> sOpt = supplierRepository.findByUser(user);
            if (sOpt.isPresent() && "ACTIVE".equals(sOpt.get().getStatus())) {
                return "redirect:/suppliers/portal";
            }
        }

        // If user is NOT an admin, show them the welcome/registration page
        if (!user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
            model.addAttribute("activePage", "suppliers");
            return "suppliers/welcome";
        }

        // ADMIN view: List all suppliers
        List<Supplier> suppliers = supplierRepository.findAll();
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("activePage", "suppliers");
        model.addAttribute("totalSuppliers", suppliers.size());
        model.addAttribute("activeCount", suppliers.stream().filter(s -> "ACTIVE".equals(s.getStatus())).count());
        model.addAttribute("reviewCount", suppliers.stream().filter(s -> "REVIEW".equals(s.getStatus())).count());

        double avg = suppliers.stream().mapToDouble(Supplier::getRating).average().orElse(0.0);
        model.addAttribute("avgRating", Math.round(avg * 10.0) / 10.0);
        
        model.addAttribute("notifications", notificationRepository.findAllByOrderByTimestampDesc());
        return "suppliers/list";
    }

    @GetMapping("/new")
    public String showAddForm(Model model, Principal principal) {
        if (principal != null) {
            User user = userService.findByEmail(principal.getName()).orElse(null);
            if (user != null && user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
                return "redirect:/suppliers";
            }
        }
        model.addAttribute("supplier", new Supplier());
        model.addAttribute("editMode", false);
        model.addAttribute("email", "");
        model.addAttribute("phone", "");
        model.addAttribute("address", "");
        model.addAttribute("activePage", "suppliers");
        return "suppliers/form";
    }

    @GetMapping("/welcome")
    public String welcome() {
        return "suppliers/welcome";
    }

    @GetMapping("/auth")
    public String supplierAuth() {
        return "suppliers/auth";
    }

    @PostMapping("/new")
    public String addSupplier(@ModelAttribute Supplier supplier, 
                            @RequestParam(required = false) String email, 
                            @RequestParam(required = false) String password, 
                            @RequestParam(required = false) String phone,
                            @RequestParam(required = false) String address,
                            Principal principal,
                            RedirectAttributes ra) {
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            ra.addFlashAttribute("error", "Email and Password are required for registration.");
            return "redirect:/suppliers/new";
        }
        if (phone == null || phone.isEmpty() || address == null || address.isEmpty()) {
            ra.addFlashAttribute("error", "Phone and Address are required for registration.");
            return "redirect:/suppliers/new";
        }
        try {
            // Determine status based on principal
            User adminPrincipal = (principal != null) ? userService.findByEmail(principal.getName()).orElse(null) : null;
            if (adminPrincipal != null && adminPrincipal.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
                supplier.setStatus("ACTIVE");
            } else {
                supplier.setStatus("REVIEW");
            }

            supplierService.registerSupplier(supplier, email, password, phone, address);
            ra.addFlashAttribute("success", "Registration successful! Welcome to the Supplier Portal.");

            // AUTO-LOGIN: Only if not already logged in (e.g. not an admin adding a supplier)
            if (principal == null) {
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
                Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
                return "redirect:/suppliers/portal";
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/suppliers/new";
        }
        
        return "redirect:/suppliers";
    }

    @GetMapping("/{id}/edit")
    public String editSupplierForm(@PathVariable Long id, Model model) {
        Supplier supplier = supplierRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid supplier Id:" + id));
        model.addAttribute("supplier", supplier);
        model.addAttribute("editMode", true);
        model.addAttribute("email", supplier.getUser() != null ? supplier.getUser().getEmail() : "");
        model.addAttribute("phone", supplier.getUser() != null ? supplier.getUser().getPhone() : "");
        model.addAttribute("address", supplier.getUser() != null ? supplier.getUser().getAddress() : "");
        model.addAttribute("activePage", "suppliers");
        return "suppliers/form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateSupplier(@PathVariable Long id, @ModelAttribute Supplier supplier, 
                               @RequestParam(required = false) String email, 
                               @RequestParam(required = false) String phone,
                               @RequestParam(required = false) String address,
                               RedirectAttributes ra) {
        try {
            Supplier existing = supplierRepository.findById(id).orElseThrow();
            existing.setName(supplier.getName());
            existing.setCategory(supplier.getCategory());
            existing.setStatus(supplier.getStatus());
            
            if (existing.getUser() != null) {
                User user = existing.getUser();
                if (email != null) user.setEmail(email);
                if (phone != null) user.setPhone(phone);
                if (address != null) user.setAddress(address);
                user.setFullName(supplier.getName());
                userRepository.save(user);
            }
            
            supplierRepository.save(existing);
            ra.addFlashAttribute("success", "Supplier updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error updating supplier: " + e.getMessage());
        }
        return "redirect:/suppliers";
    }

    @PostMapping("/delete-account")
    @PreAuthorize("isAuthenticated()")
    public String deleteMyAccount(Principal principal, RedirectAttributes ra) {
        try {
            User currentUser = userService.findByEmail(principal.getName()).orElseThrow();
            
            // Safety check: ensure user is actually a supplier
            if (!currentUser.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_SUPPLIER"))) {
                ra.addFlashAttribute("error", "You do not have an active supplier account to delete.");
                return "redirect:/dashboard";
            }
            supplierService.deleteSupplierAccount(currentUser);
            
            // Note: The user stays logged in but ROLE_SUPPLIER is removed.
            // A redirect to dashboard will reflect the change as the sidebar checks roles.
            ra.addFlashAttribute("success", "Supplier features removed successfully. You are now a normal user.");
            return "redirect:/dashboard";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error removing supplier features: " + e.getMessage());
            return "redirect:/suppliers/portal";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteSupplier(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Supplier supplier = supplierRepository.findById(id).orElseThrow();
            supplierRepository.delete(supplier);
            ra.addFlashAttribute("success", "Supplier removed successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error deleting supplier: " + e.getMessage());
        }
        return "redirect:/suppliers";
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public String activateSupplier(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Supplier supplier = supplierRepository.findById(id).orElseThrow();
            supplierService.activateSupplier(supplier);
            ra.addFlashAttribute("success", "Supplier features granted to " + supplier.getName());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error activating supplier: " + e.getMessage());
        }
        return "redirect:/suppliers";
    }

    @GetMapping("/contracts")
    public String listContracts(Model model, Principal principal, RedirectAttributes ra) {
        if (principal == null) {
            ra.addFlashAttribute("error", "First, you must register as a supplier before accessing the supplier-related features.");
            return "redirect:/suppliers";
        }
        List<Contract> contracts;
        List<SupplyRequest> pendingRequests = new ArrayList<>();
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        
        if (user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
            contracts = contractRepository.findAll().stream()
                .filter(c -> c.getTitle() != null && c.getTitle().startsWith("Supply Agreement:"))
                .collect(Collectors.toList());
            pendingRequests = supplyRequestRepository.findByStatus("PENDING");
        } else {
            // Find the supplier associated with this user
            Optional<Supplier> supplierOpt = supplierRepository.findByUser(user);
            if (supplierOpt.isEmpty()) {
                ra.addFlashAttribute("error", "First, you must register as a supplier before accessing the supplier-related features.");
                return "redirect:/suppliers";
            }
            Supplier supplier = supplierOpt.get();
            
            // SECURITY CHECK: Must be ACTIVE
            if (!"ACTIVE".equals(supplier.getStatus())) {
                ra.addFlashAttribute("error", "Your supplier account is currently under review. Access to features will be granted once approved.");
                return "redirect:/suppliers";
            }

            contracts = contractRepository.findBySupplier(supplier).stream()
                .filter(c -> c.getTitle() != null && c.getTitle().startsWith("Supply Agreement:"))
                .collect(Collectors.toList());
            pendingRequests = supplyRequestRepository.findBySupplierAndStatus(supplier, "PENDING");
        }

        model.addAttribute("contracts", contracts);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("activePage", "contracts");
        
        model.addAttribute("pendingCount", contracts.stream().filter(c -> "PENDING".equals(c.getStatus())).count() + pendingRequests.size());
        model.addAttribute("approvedCount", contracts.stream().filter(c -> "APPROVED".equals(c.getStatus())).count());
        model.addAttribute("activeCount", contracts.stream().filter(c -> "ACTIVE".equals(c.getStatus())).count());
        
        long expiring = contracts.stream()
            .filter(c -> c.getExpiresAt() != null && c.getExpiresAt().isBefore(LocalDate.now().plusMonths(1)))
            .count();
        model.addAttribute("expiringCount", expiring);
        
        return "suppliers/contracts";
    }

    @GetMapping("/portal")
    public String supplierPortal(Model model, Principal principal, RedirectAttributes ra) {
        if (principal == null) {
            ra.addFlashAttribute("error", "First, you must register as a supplier before accessing the supplier-related features.");
            return "redirect:/suppliers";
        }
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        List<SupplierOrder> orders;
        List<SupplyRequest> approvedDeals = new ArrayList<>();
        List<Notification> notifications = notificationRepository.findAllByOrderByTimestampDesc();
        List<RawMaterial> products;

        if (user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
            orders = supplierOrderRepository.findAll();
            approvedDeals = supplyRequestRepository.findByStatus("APPROVED");
            products = rawMaterialRepository.findAll();
        } else {
            Optional<Supplier> supplierOpt = supplierRepository.findByUser(user);
            if (supplierOpt.isEmpty()) {
                ra.addFlashAttribute("error", "First, you must register as a supplier before accessing the supplier-related features.");
                return "redirect:/suppliers";
            }
            Supplier supplier = supplierOpt.get();

            // SECURITY CHECK: Must be ACTIVE
            if (!"ACTIVE".equals(supplier.getStatus())) {
                ra.addFlashAttribute("error", "Your supplier account is currently under review. Access to features will be granted once approved.");
                return "redirect:/suppliers";
            }

            orders = supplierOrderRepository.findBySupplier(supplier);
            approvedDeals = supplyRequestRepository.findBySupplierAndStatus(supplier, "APPROVED");
            products = rawMaterialRepository.findAll(); 
            model.addAttribute("supplier", supplier);
        }

        model.addAttribute("orders", orders);
        model.addAttribute("approvedDeals", approvedDeals);
        model.addAttribute("notifications", notifications);
        model.addAttribute("products", products);
        model.addAttribute("activePage", "supplier-portal");

        long newOrders = orders.stream().filter(o -> "NEW".equals(o.getStatus())).count() + approvedDeals.size();
        long deliveredCount = orders.stream().filter(o -> "DELIVERED".equals(o.getStatus())).count();
        long activeProducts = products.stream().filter(p -> p.getQuantity() > 0).count();
        long unreadAlerts = notifications.size();

        model.addAttribute("newOrders", newOrders);
        model.addAttribute("deliveredCount", deliveredCount);
        model.addAttribute("activeProducts", activeProducts);
        model.addAttribute("unreadAlerts", unreadAlerts);
        return "suppliers/portal";
    }

    @GetMapping("/profile")
    public String showProfile(Model model, Principal principal, RedirectAttributes ra) {
        if (principal == null) {
            ra.addFlashAttribute("error", "First, you must register as a supplier before accessing the supplier-related features.");
            return "redirect:/suppliers";
        }
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        Optional<Supplier> supplierOpt = supplierRepository.findByUser(user);
        if (supplierOpt.isEmpty()) {
            ra.addFlashAttribute("error", "First, you must register as a supplier before accessing the supplier-related features.");
            return "redirect:/suppliers";
        }
        Supplier supplier = supplierOpt.get();
        
        // SECURITY CHECK: Must be ACTIVE
        if (!"ACTIVE".equals(supplier.getStatus())) {
            ra.addFlashAttribute("error", "Your supplier account is currently under review. Access to features will be granted once approved.");
            return "redirect:/suppliers";
        }

        model.addAttribute("supplier", supplier);
        model.addAttribute("user", user);
        model.addAttribute("activePage", "supplier-profile");
        return "suppliers/profile";
    }

    @PostMapping("/profile")
    @PreAuthorize("hasAuthority('SUPPLIER_ACCESS')")
    public String updateProfile(@ModelAttribute Supplier supplier,
                                @RequestParam String fullName,
                                @RequestParam String phone,
                                @RequestParam String address,
                                Principal principal,
                                RedirectAttributes ra) {
        if (fullName.isBlank() || phone.isBlank() || address.isBlank() || supplier.getName().isBlank()) {
            ra.addFlashAttribute("error", "All fields are required.");
            return "redirect:/suppliers/profile";
        }

        User user = userService.findByEmail(principal.getName()).orElseThrow();
        Supplier existing = supplierRepository.findByUser(user).orElseThrow();

        // Update Supplier fields
        existing.setName(supplier.getName());
        existing.setCategory(supplier.getCategory());
        
        // Update User fields
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setAddress(address);

        userRepository.save(user);
        supplierRepository.save(existing);

        ra.addFlashAttribute("success", "Profile updated successfully.");
        return "redirect:/suppliers/profile";
    }

    @PostMapping("/profile/delete")
    @PreAuthorize("hasAuthority('SUPPLIER_ACCESS')")
    public String deleteAccount(Principal principal, RedirectAttributes ra) {
        try {
            User user = userService.findByEmail(principal.getName()).orElseThrow();
            supplierService.deleteSupplierAccount(user);
            ra.addFlashAttribute("success", "Your supplier account has been deleted successfully.");
            return "redirect:/suppliers/welcome";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error deleting account: " + e.getMessage());
            return "redirect:/suppliers/profile";
        }
    }

    @GetMapping("/notifications/new")
    public String showNotificationForm(Model model) {
        model.addAttribute("activePage", "suppliers");
        return "suppliers/notification_form";
    }

    @PostMapping("/notifications/new")
    public String sendNotification(@RequestParam String title, @RequestParam String message, 
                                 @RequestParam String type, RedirectAttributes ra) {
        Notification note = new Notification(title, message, type);
        notificationRepository.save(note);
        ra.addFlashAttribute("success", "Global notification sent successfully.");
        return "redirect:/suppliers";
    }

    @PostMapping("/products/toggle-availability/{id}")
    public String toggleAvailability(@PathVariable Long id, RedirectAttributes ra) {
        Optional<RawMaterial> prodOpt = rawMaterialRepository.findById(id);
        if (prodOpt.isPresent()) {
            RawMaterial prod = prodOpt.get();
            // Toggle logic: if qty > 0 set to 0, if 0 set to some default (e.g. 10)
            if (prod.getQuantity() > 0) {
                prod.setQuantity(0);
                prod.setStatus("Out of Stock");
            } else {
                prod.setQuantity(10);
                prod.setStatus("In Stock");
            }
            rawMaterialRepository.save(prod);
            ra.addFlashAttribute("success", "Availability updated for " + prod.getName());
        }
        return "redirect:/suppliers/portal";
    }

    @PostMapping("/orders/request")
    @PreAuthorize("hasRole('ADMIN')")
    public String requestOrder(@RequestParam Long supplierId, @RequestParam String item, @RequestParam String quantity, RedirectAttributes ra) {
        Supplier supplier = supplierRepository.findById(supplierId).orElseThrow();
        SupplierOrder order = new SupplierOrder();
        order.setId("REQ-" + (int)(Math.random() * 900 + 100));
        order.setItem(item);
        order.setQuantity(quantity);
        order.setStatus("REQUESTED"); // Admin asking
        order.setSupplier(supplier);
        supplierOrderRepository.save(order);
        
        ra.addFlashAttribute("success", "Order request sent to " + supplier.getName());
        return "redirect:/suppliers";
    }

    @PostMapping("/orders/{id}/accept")
    public String acceptOrder(@PathVariable String id, RedirectAttributes ra) {
        SupplierOrder order = supplierOrderRepository.findById(id).orElseThrow();
        order.setStatus("NEW"); // Becomes a formal order
        supplierOrderRepository.save(order);

        // AUTOMATION: Update Raw Materials Inventory immediately upon acceptance
        int quantityToAdd = extractNumericQuantity(order.getQuantity());
        if (quantityToAdd > 0) {
            Optional<RawMaterial> materialOpt = rawMaterialRepository.findByNameIgnoreCase(order.getItem());
            RawMaterial material;
            if (materialOpt.isPresent()) {
                material = materialOpt.get();
                int newTotal = material.getQuantity() + quantityToAdd;
                
                // Update supplier name and stock using service for proper logging
                material.setSupplier(order.getSupplier().getName());
                rawMaterialRepository.save(material);
                
                inventoryService.updateRawMaterialStock(material.getId(), newTotal, "Automated restock from accepted order: " + id);
                ra.addFlashAttribute("success", "Order accepted! " + quantityToAdd + " units of " + order.getItem() + " added to inventory.");
            } else {
                // Create new material if it doesn't exist
                material = new RawMaterial();
                material.setName(order.getItem());
                material.setQuantity(0); // Set to 0 first to use updateRawMaterialStock correctly
                material.setSupplier(order.getSupplier().getName());
                material.setCategory("Uncategorized");
                material.setStatus("In Stock");
                rawMaterialRepository.save(material);
                
                inventoryService.updateRawMaterialStock(material.getId(), quantityToAdd, "Initial stock from accepted order: " + id);
                ra.addFlashAttribute("success", "Order accepted! New material " + order.getItem() + " created and restocked.");
            }
        } else {
            ra.addFlashAttribute("success", "You have accepted the order request!");
        }
        
        return "redirect:/suppliers/portal";
    }

    private int extractNumericQuantity(String quantity) {
        if (quantity == null || quantity.isEmpty()) return 0;
        // Extract first numeric sequence (e.g. "100 units" -> 100)
        String numericPart = quantity.replaceAll("[^0-9]", "");
        if (numericPart.isEmpty()) return 0;
        try {
            return Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @PostMapping("/orders/{id}/reject")
    public String rejectOrder(@PathVariable String id, RedirectAttributes ra) {
        SupplierOrder order = supplierOrderRepository.findById(id).orElseThrow();
        order.setStatus("REJECTED");
        supplierOrderRepository.save(order);
        ra.addFlashAttribute("success", "Order request declined.");
        return "redirect:/suppliers/portal";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable String id, @RequestParam String status, RedirectAttributes ra) {
        Optional<SupplierOrder> orderOpt = supplierOrderRepository.findById(id);
        if (orderOpt.isPresent()) {
            SupplierOrder order = orderOpt.get();
            order.setStatus(status);
            supplierOrderRepository.save(order);
            ra.addFlashAttribute("success", "Order " + id + " updated to " + status);
        }
        return "redirect:/suppliers/portal";
    }

    @PostMapping("/contracts/{id}/status")
    public String updateContractStatus(@PathVariable Long id, @RequestParam String status, RedirectAttributes ra) {
        Optional<Contract> contractOpt = contractRepository.findById(id);
        if (contractOpt.isPresent()) {
            Contract contract = contractOpt.get();
            contract.setStatus(status);
            contractRepository.save(contract);
            ra.addFlashAttribute("success", "Contract updated to " + status);
        }
        return "redirect:/suppliers/contracts";
    }

    @GetMapping("/contracts/new")
    public String showContractForm(Model model) {
        model.addAttribute("contract", new Contract());
        model.addAttribute("suppliers", supplierRepository.findAll());
        model.addAttribute("activePage", "contracts");
        return "suppliers/contracts_form";
    }

    @PostMapping("/contracts/new")
    public String addContract(@ModelAttribute Contract contract, RedirectAttributes ra) {
        contract.setStatus("PENDING");
        contractRepository.save(contract);
        ra.addFlashAttribute("success", "Contract uploaded successfully.");
        return "redirect:/suppliers/contracts";
    }
    @GetMapping("/supply-offers")
    public String supplyOffers(Model model, Principal principal, RedirectAttributes ra) {
        if (principal == null) {
            ra.addFlashAttribute("error", "First, you must register as a supplier before accessing the supplier-related features.");
            return "redirect:/suppliers";
        }
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        
        if (!user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
            Optional<Supplier> supplierOpt = supplierRepository.findByUser(user);
            if (supplierOpt.isEmpty()) {
                ra.addFlashAttribute("error", "First, you must register as a supplier before accessing the supplier-related features.");
                return "redirect:/suppliers";
            }
            
            // SECURITY CHECK: Must be ACTIVE
            if (!"ACTIVE".equals(supplierOpt.get().getStatus())) {
                ra.addFlashAttribute("error", "Your supplier account is currently under review. Access to features will be granted once approved.");
                return "redirect:/suppliers";
            }
        }
        List<RawMaterial> allMaterials = rawMaterialRepository.findAll();
        List<com.hotelcraft.model.MaterialCategory> categories = categoryRepository.findAll();
        
        // Use a map to track unique materials by name
        Map<String, Map<String, Object>> marketplace = new HashMap<>();
        
        // 1. Add all defined categories as potential requirements
        for (com.hotelcraft.model.MaterialCategory cat : categories) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", cat.getName());
            map.put("category", cat.getName());
            map.put("quantity", 0);
            map.put("id", null); // No physical record yet
            marketplace.put(cat.getName().toLowerCase(), map);
        }
        
        // 2. Overlay actual inventory data
        for (RawMaterial rm : allMaterials) {
            String key = rm.getName().toLowerCase();
            if (marketplace.containsKey(key)) {
                Map<String, Object> map = marketplace.get(key);
                map.put("quantity", (int)map.get("quantity") + rm.getQuantity());
                if (map.get("id") == null) map.put("id", rm.getId());
            } else {
                Map<String, Object> map = new HashMap<>();
                map.put("name", rm.getName());
                map.put("category", rm.getCategory() != null ? rm.getCategory() : "Raw Material");
                map.put("quantity", rm.getQuantity());
                map.put("id", rm.getId());
                marketplace.put(key, map);
            }
        }
            
        List<Map<String, Object>> displayMaterials = new ArrayList<>(marketplace.values());
        List<SupplyRequest> myRequests = new ArrayList<>();
        
        if (user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_SUPPLIER"))) {
            Optional<Supplier> supplierOpt = supplierRepository.findByUser(user);
            if (supplierOpt.isPresent()) {
                myRequests = supplyRequestRepository.findBySupplier(supplierOpt.get());
            }
        } else if (user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
            myRequests = supplyRequestRepository.findAll();
        }

        model.addAttribute("materials", displayMaterials);
        model.addAttribute("myRequests", myRequests);
        model.addAttribute("activePage", "supply-offers");
        model.addAttribute("activeTab", "supply-offers");
        return "suppliers/supply_offers";
    }

    @PostMapping("/supply-request/new")
    @PreAuthorize("hasAuthority('SUPPLIER_OFFER_CREATE')")
    public String createSupplyRequest(@RequestParam(required = false) Long materialId, @RequestParam String materialName, @RequestParam Integer quantity, @RequestParam Double price, Principal principal, RedirectAttributes ra) {
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        Optional<Supplier> supplierOpt = supplierRepository.findByUser(user);
        
        if (supplierOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Only registered suppliers can submit quotes.");
            return "redirect:/suppliers/supply-offers";
        }
        Supplier supplier = supplierOpt.get();
        
        RawMaterial material;
        if (materialId != null) {
            material = rawMaterialRepository.findById(materialId).orElse(null);
        } else {
            material = rawMaterialRepository.findByNameIgnoreCase(materialName).orElse(null);
        }

        // If material doesn't exist in inventory, create a placeholder record
        if (material == null) {
            material = new RawMaterial();
            material.setName(materialName);
            material.setQuantity(0);
            material.setStatus("Out of Stock");
            material.setCategory(materialName); // Use the specific category name
            rawMaterialRepository.save(material);
        }

        SupplyRequest request = new SupplyRequest();
        request.setSupplier(supplier);
        request.setMaterial(material);
        request.setOfferedQuantity(quantity);
        request.setPricePerUnit(price);
        request.setStatus("PENDING");
        
        supplyRequestRepository.save(request);
        ra.addFlashAttribute("success", "Supply offer for " + material.getName() + " sent to Admin for approval.");
        return "redirect:/suppliers/supply-offers";
    }

    @PostMapping("/supply-request/{id}/approve")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String approveSupplyRequest(@PathVariable Long id, @RequestParam Integer approvedQuantity, RedirectAttributes ra) {
        SupplyRequest request = supplyRequestRepository.findById(id).orElseThrow();
        request.setApprovedQuantity(approvedQuantity);
        request.setStatus("APPROVED");
        supplyRequestRepository.save(request);

        // Create a formal Contract for this supply deal
        Contract contract = new Contract();
        contract.setTitle("Supply Agreement: " + request.getMaterial().getName());
        contract.setSupplier(request.getSupplier());
        contract.setValue(request.getPricePerUnit() * approvedQuantity);
        contract.setExpiresAt(LocalDate.now().plusMonths(1)); // Default 1 month
        contract.setStatus("PENDING"); // Pending actual receipt
        contractRepository.save(contract);
        
        ra.addFlashAttribute("success", "Deal accepted! A new contract has been generated for " + request.getSupplier().getName());
        return "redirect:/suppliers/contracts";
    }

    @PostMapping("/supply-request/{id}/reject")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String rejectSupplyRequest(@PathVariable Long id, RedirectAttributes ra) {
        SupplyRequest request = supplyRequestRepository.findById(id).orElseThrow();
        request.setStatus("REJECTED");
        supplyRequestRepository.save(request);
        ra.addFlashAttribute("success", "Supply request rejected.");
        return "redirect:/suppliers/contracts";
    }

    @PostMapping("/supply-request/{id}/activate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String activateDeal(@PathVariable Long id, RedirectAttributes ra) {
        SupplyRequest request = supplyRequestRepository.findById(id).orElseThrow();
        if (!"APPROVED".equals(request.getStatus())) {
            ra.addFlashAttribute("error", "Only approved requests can be marked as received.");
            return "redirect:/suppliers/portal";
        }

        // Update the actual inventory
        RawMaterial material = request.getMaterial();
        int approvedQty = request.getApprovedQuantity() != null ? request.getApprovedQuantity() : request.getOfferedQuantity();
        material.setQuantity(material.getQuantity() + approvedQty);
        material.setSupplier(request.getSupplier().getName()); // Set the supplier name
        material.setStatus("In Stock");
        rawMaterialRepository.save(material);

        request.setStatus("RECEIVED");
        supplyRequestRepository.save(request);

        // Update the contract to ACTIVE if one exists for this material/supplier
        List<Contract> contracts = contractRepository.findAll();
        for (Contract c : contracts) {
            if (c.getSupplier().getId().equals(request.getSupplier().getId()) && 
                c.getTitle().contains(request.getMaterial().getName()) && 
                "PENDING".equals(c.getStatus())) {
                c.setStatus("ACTIVE");
                contractRepository.save(c);
                break;
            }
        }
        
        ra.addFlashAttribute("success", "Inventory updated! " + approvedQty + " units of " + material.getName() + " added to stock.");
        return "redirect:/suppliers/portal";
    }
}
