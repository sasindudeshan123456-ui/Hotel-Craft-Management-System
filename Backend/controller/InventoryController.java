package com.hotelcraft.controller;

import com.hotelcraft.model.Product;
import com.hotelcraft.model.ProductMaterial;
import com.hotelcraft.model.RawMaterial;
import com.hotelcraft.service.InventoryService;
import com.hotelcraft.service.UserService;
import com.hotelcraft.model.User;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.web.multipart.MultipartFile;
 
import java.io.File;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private com.hotelcraft.repository.MaterialCategoryRepository categoryRepository;
 
    @Autowired
    private UserService userService;

    @GetMapping("/finished-products")
    public String finishedProducts(Model model, Principal principal, RedirectAttributes ra) {
        if (principal == null) {
            ra.addFlashAttribute("error", "Please login to view detailed inventory.");
            return "redirect:/login";
        }
        
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        if (!user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN")) &&
            !user.getRoles().stream().anyMatch(r -> r.getPermissions().stream().anyMatch(p -> p.getName().equals("INVENTORY_READ_ALL")))) {
             ra.addFlashAttribute("error", "You only have access to the Stock Summary. Detailed inventory is restricted.");
             return "redirect:/inventory/raw-materials";
        }
        
        inventoryService.mergeDuplicateProducts(); // Clean up existing duplicates
        model.addAttribute("products", inventoryService.getAllProducts());
        return "inventory/finished-products";
    }

    @GetMapping("/finished-products/new")
    @PreAuthorize("hasAuthority('INVENTORY_CREATE')")
    public String newProductForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("allMaterials", inventoryService.getAllRawMaterials());
        return "inventory/finished-products-form";
    }

    @PostMapping("/finished-products/new")
    @PreAuthorize("hasAuthority('INVENTORY_CREATE')")
    public String createProduct(Product product, @RequestParam(value = "image", required = false) MultipartFile image, @RequestParam(value = "materialIds", required = false) List<Long> materialIds, @RequestParam(value = "materialQuantities", required = false) List<Integer> quantities, @RequestParam(value = "customerName", required = false) String customerName, RedirectAttributes redirectAttributes, Model model) {
        // Validation: Materials are mandatory
        if (materialIds == null || materialIds.isEmpty()) {
            model.addAttribute("product", product);
            model.addAttribute("allMaterials", inventoryService.getAllRawMaterials());
            model.addAttribute("error", "At least one material must be specified for a new product.");
            return "inventory/finished-products-form";
        }

        try {
            Product existing = inventoryService.findProductByName(product.getName());
            if (existing != null) {
                // Aggregate quantity
                int newTotal = existing.getQuantity() + product.getQuantity();
                
                // Use specialized method for stock updates to trigger deductions and logging
                inventoryService.updateProductStock(existing.getId(), newTotal, "Aggregated from new entry", customerName);
                
                // Allow photo update during aggregation if a new one is provided
                if (image != null && !image.isEmpty()) {
                    String path = saveProductImage(image);
                    if (path != null) {
                        existing.setImagePath(path);
                        inventoryService.saveProduct(existing);
                    }
                }
                redirectAttributes.addFlashAttribute("success", "Stock aggregated successfully.");
            } else {
                // Handle image upload
                if (image != null && !image.isEmpty()) {
                    String path = saveProductImage(image);
                    if (path != null) product.setImagePath(path);
                }

                // Atomic production: Includes saving product, BOM, and initial stock deduction
                inventoryService.produceNewProduct(product, materialIds, quantities, customerName);
                redirectAttributes.addFlashAttribute("success", "Product created and materials deducted successfully.");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Production Stopped: " + e.getMessage());
            return "redirect:/inventory/finished-products";
        }
        return "redirect:/inventory/finished-products";
    }

    @GetMapping("/finished-products/edit/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE')")
    public String editProductForm(@PathVariable("id") Long id, Model model) {
        Product product = inventoryService.getProductById(id);
        if (product == null) {
            return "redirect:/inventory/finished-products";
        }
        model.addAttribute("product", product);
        return "inventory/finished-products-edit";
    }

    @PostMapping("/finished-products/edit/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE')")
    public String updateProduct(@PathVariable("id") Long id, Product productDetails, @RequestParam(value = "image", required = false) MultipartFile image, @RequestParam(value = "remarks", required = false) String remarks, @RequestParam(value = "customerName", required = false) String customerName, RedirectAttributes redirectAttributes) {
        Product existing = inventoryService.getProductById(id);
        if (existing != null) {
            try {
                // Use specialized method for stock updates to trigger deductions and validation
                inventoryService.updateProductStock(id, productDetails.getQuantity(), remarks != null ? remarks : "Manual update", customerName);
                
                // Update other fields only if stock update was successful
                existing.setName(productDetails.getName());
                existing.setCategory(productDetails.getCategory());
                existing.setPrice(productDetails.getPrice());
                existing.setDiscount(productDetails.getDiscount());
                existing.setDescription(productDetails.getDescription());
                
                // Handle image upload
                if (image != null && !image.isEmpty()) {
                    String path = saveProductImage(image);
                    if (path != null) existing.setImagePath(path);
                }
                
                inventoryService.saveProduct(existing); 
                redirectAttributes.addFlashAttribute("success", "Product updated and inventory synced.");
            } catch (RuntimeException e) {
                redirectAttributes.addFlashAttribute("error", "Update Failed: " + e.getMessage());
                return "redirect:/inventory/finished-products";
            }
        }
        return "redirect:/inventory/finished-products";
    }

    @PostMapping("/finished-products/delete/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_DELETE')")
    public String deleteProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        inventoryService.deleteProduct(id);
        redirectAttributes.addFlashAttribute("success", "Product deleted successfully.");
        return "redirect:/inventory/finished-products";
    }

    @GetMapping("/raw-materials")
    public String rawMaterials(Model model, Principal principal, RedirectAttributes ra) {
        if (principal == null) {
            ra.addFlashAttribute("error", "Please login to view stock information.");
            return "redirect:/login";
        }
        
        // Correct existing negative stocks and merge duplicates
        inventoryService.cleanupNegativeStocks();
        inventoryService.mergeDuplicateRawMaterials();
        List<RawMaterial> materials = inventoryService.getAllRawMaterials();
        
        // Data Correction: Fix materials stuck in "Raw Material" placeholder category
        materials.stream()
            .filter(m -> "Raw Material".equals(m.getCategory()))
            .forEach(m -> {
                if (categoryRepository.findByName(m.getName()).isPresent()) {
                    m.setCategory(m.getName());
                    inventoryService.saveRawMaterial(m);
                }
            });
            
        // Re-fetch materials after potential updates
        materials = inventoryService.getAllRawMaterials();
        
        model.addAttribute("materials", materials);
        model.addAttribute("totalItemsCount", materials.size());

        // Get all dynamic categories
        java.util.List<com.hotelcraft.model.MaterialCategory> categories = categoryRepository.findAll();

        // Group materials by category for summaries
        java.util.Map<String, java.util.List<RawMaterial>> groupedMaterials = materials.stream()
                .collect(java.util.stream.Collectors
                        .groupingBy(m -> m.getCategory() != null ? m.getCategory() : "Uncategorized"));

        // Calculate summaries for each category existing in DB
        java.util.Map<String, java.util.Map<String, Object>> categorySummaries = new java.util.LinkedHashMap<>();

        categories.forEach(cat -> {
            java.util.List<RawMaterial> items = groupedMaterials.getOrDefault(cat.getName(),
                    new java.util.ArrayList<>());
            categorySummaries.put(cat.getName(), calculateCategorySummary(items, cat));
        });

        // Add "Uncategorized" if items exist
        if (groupedMaterials.containsKey("Uncategorized")) {
            categorySummaries.put("Uncategorized",
                    calculateCategorySummary(groupedMaterials.get("Uncategorized"), null));
        }

        model.addAttribute("categorySummaries", categorySummaries);
        model.addAttribute("categoryCount", categories.size());
        return "inventory/raw-materials";
    }

    private java.util.Map<String, Object> calculateCategorySummary(java.util.List<RawMaterial> items,
            com.hotelcraft.model.MaterialCategory cat) {
        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        int totalQty = items.stream().mapToInt(RawMaterial::getQuantity).sum();
        summary.put("totalQuantity", totalQty);

        // Default thresholds if category is null
        int outLimit = (cat != null && cat.getOutOfStockThreshold() != null) ? cat.getOutOfStockThreshold() : 5;
        int lowLimit = (cat != null && cat.getLowStockThreshold() != null) ? cat.getLowStockThreshold() : 10;

        // Calculate Aggregate Status: Based on total category quantity vs boundaries
        String aggregateStatus;
        if (totalQty < outLimit) {
            aggregateStatus = "Out of Stock";
        } else if (totalQty <= lowLimit) {
            aggregateStatus = "Low Stock";
        } else {
            aggregateStatus = "In Stock";
        }

        summary.put("status", aggregateStatus);
        summary.put("itemCount", items.size());
        return summary;
    }

    // Category Management
    @GetMapping("/raw-materials/categories")
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE')")
    public String manageCategories(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("newCategory", new com.hotelcraft.model.MaterialCategory());
        return "inventory/categories";
    }

    @PostMapping("/raw-materials/categories/new")
    @PreAuthorize("hasAuthority('INVENTORY_CREATE')")
    public String addCategory(com.hotelcraft.model.MaterialCategory category, RedirectAttributes redirectAttributes) {
        if (categoryRepository.findByName(category.getName()).isEmpty()) {
            categoryRepository.save(category);
            redirectAttributes.addFlashAttribute("success", "Category added successfully.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Category already exists.");
        }
        return "redirect:/inventory/raw-materials/categories";
    }

    @PostMapping("/raw-materials/categories/delete/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_DELETE')")
    public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        categoryRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Category deleted successfully.");
        return "redirect:/inventory/raw-materials/categories";
    }

    @GetMapping("/raw-materials/categories/edit/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE')")
    public String editCategoryForm(@PathVariable("id") Long id, Model model) {
        java.util.Optional<com.hotelcraft.model.MaterialCategory> category = categoryRepository.findById(id);
        if (category.isEmpty()) {
            return "redirect:/inventory/raw-materials/categories";
        }
        model.addAttribute("category", category.get());
        return "inventory/category-edit";
    }

    @PostMapping("/raw-materials/categories/edit/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE')")
    public String updateCategory(@PathVariable("id") Long id, com.hotelcraft.model.MaterialCategory categoryDetails,
            RedirectAttributes redirectAttributes) {
        java.util.Optional<com.hotelcraft.model.MaterialCategory> existingOpt = categoryRepository.findById(id);
        if (existingOpt.isPresent()) {
            com.hotelcraft.model.MaterialCategory existing = existingOpt.get();
            String oldName = existing.getName();
            existing.setName(categoryDetails.getName());
            existing.setLowStockThreshold(categoryDetails.getLowStockThreshold());
            existing.setOutOfStockThreshold(categoryDetails.getOutOfStockThreshold());
            categoryRepository.save(existing);

            // If name changed, update all materials with this category string
            if (!oldName.equals(categoryDetails.getName())) {
                inventoryService.getAllRawMaterials().stream()
                        .filter(m -> oldName.equals(m.getCategory()))
                        .forEach(m -> {
                            m.setCategory(categoryDetails.getName());
                            inventoryService.saveRawMaterial(m);
                        });
            }

            // Force recalculate statuses for all items in this category
            inventoryService.getAllRawMaterials().stream()
                    .filter(m -> existing.getName().equals(m.getCategory()))
                    .forEach(m -> {
                        m.setStatus(calculateStatus(m));
                        inventoryService.saveRawMaterial(m);
                    });

            redirectAttributes.addFlashAttribute("success", "Category updated and stock alerts recalculated.");
        }
        return "redirect:/inventory/raw-materials/categories";
    }

    @GetMapping("/raw-materials/new")
    @PreAuthorize("hasAuthority('INVENTORY_CREATE')")
    public String newRawMaterialForm(Model model) {
        model.addAttribute("material", new RawMaterial());
        model.addAttribute("categories", categoryRepository.findAll());
        return "inventory/raw-materials-form";
    }

    @PostMapping("/raw-materials/new")
    @PreAuthorize("hasAuthority('INVENTORY_CREATE')")
    public String addRawMaterial(RawMaterial material, RedirectAttributes redirectAttributes) {
        RawMaterial existing = inventoryService.findRawMaterialByName(material.getName());
        if (existing != null) {
            // Aggregate quantity
            int newTotal = existing.getQuantity() + material.getQuantity();
            inventoryService.updateRawMaterialStock(existing.getId(), newTotal, "Aggregated restock from new entry");
            redirectAttributes.addFlashAttribute("success", "Material already exists. Quantity aggregated successfully.");
        } else {
            // Auto-calculate status based on thresholds
            material.setStatus(calculateStatus(material));
            inventoryService.saveRawMaterial(material);
            if (material.getQuantity() > 0) {
                inventoryService.updateRawMaterialStock(material.getId(), material.getQuantity(), "Initial material setup");
            }
            redirectAttributes.addFlashAttribute("success", "Material added successfully.");
        }
        return "redirect:/inventory/raw-materials";
    }

    @GetMapping("/raw-materials/edit/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE')")
    public String editRawMaterialForm(@PathVariable("id") Long id, Model model) {
        RawMaterial material = inventoryService.getRawMaterialById(id);
        if (material == null) {
            return "redirect:/inventory/raw-materials";
        }
        model.addAttribute("material", material);
        model.addAttribute("categories", categoryRepository.findAll());
        return "inventory/raw-materials-edit";
    }

    @PostMapping("/raw-materials/edit/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE')")
    public String updateRawMaterial(@PathVariable("id") Long id, RawMaterial materialDetails, @RequestParam(value = "remarks", required = false) String remarks,
            RedirectAttributes redirectAttributes) {
        RawMaterial existing = inventoryService.getRawMaterialById(id);
        if (existing != null) {
            existing.setName(materialDetails.getName());
            existing.setSupplier(materialDetails.getSupplier());
            existing.setCategory(materialDetails.getCategory());

            // Use specialized method for stock updates to trigger logging
            inventoryService.updateRawMaterialStock(id, materialDetails.getQuantity(), remarks != null ? remarks : "Manual update");

            existing.setStatus(calculateStatus(existing));
            inventoryService.saveRawMaterial(existing);
            redirectAttributes.addFlashAttribute("success", "Material updated successfully.");
        }
        return "redirect:/inventory/raw-materials";
    }

    @PostMapping("/raw-materials/delete/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_DELETE')")
    public String deleteRawMaterial(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        inventoryService.deleteRawMaterial(id);
        redirectAttributes.addFlashAttribute("success", "Material deleted successfully.");
        return "redirect:/inventory/raw-materials";
    }

    @GetMapping("/history/product/{id}")
    public String productHistory(@PathVariable("id") Long id, Model model, Principal principal, RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        if (!user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
            ra.addFlashAttribute("error", "Access denied to inventory history.");
            return "redirect:/inventory/raw-materials";
        }
        
        Product product = inventoryService.getProductById(id);
        if (product == null) return "redirect:/inventory/finished-products";
        
        model.addAttribute("product", product);
        model.addAttribute("history", inventoryService.getProductHistory(id));
        return "inventory/inventory-history";
    }

    @GetMapping("/history/material/{id}")
    public String materialHistory(@PathVariable("id") Long id, Model model, Principal principal, RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        if (!user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
            ra.addFlashAttribute("error", "Access denied to inventory history.");
            return "redirect:/inventory/raw-materials";
        }
        
        RawMaterial material = inventoryService.getRawMaterialById(id);
        if (material == null) return "redirect:/inventory/raw-materials";
        
        model.addAttribute("material", material);
        model.addAttribute("history", inventoryService.getRawMaterialHistory(id));
        return "inventory/inventory-history";
    }

    // BOM Management
    @GetMapping("/finished-products/bom/{id}")
    public String manageBOM(@PathVariable("id") Long id, Model model, Principal principal, RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        if (!user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
            ra.addFlashAttribute("error", "Access denied to BOM management.");
            return "redirect:/inventory/raw-materials";
        }
        
        Product product = inventoryService.getProductById(id);
        if (product == null) return "redirect:/inventory/finished-products";
        
        model.addAttribute("product", product);
        model.addAttribute("bom", inventoryService.getProductBOM(id));
        model.addAttribute("allMaterials", inventoryService.getAllRawMaterials());
        return "inventory/product-bom";
    }

    @PostMapping("/finished-products/bom/{id}/add")
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE')")
    public String addBOMItem(@PathVariable("id") Long id, @RequestParam("materialId") Long materialId, @RequestParam("quantity") Double quantity, RedirectAttributes redirectAttributes) {
        Product product = inventoryService.getProductById(id);
        RawMaterial material = inventoryService.getRawMaterialById(materialId);
        
        if (product != null && material != null) {
            ProductMaterial pm = new ProductMaterial(product, material, quantity);
            inventoryService.saveProductMaterial(pm);
            redirectAttributes.addFlashAttribute("success", "Material added to BOM.");
        }
        return "redirect:/inventory/finished-products/bom/" + id;
    }

    @PostMapping("/finished-products/bom/delete/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE')")
    public String deleteBOMItem(@PathVariable("id") Long id, @RequestParam("productId") Long productId, RedirectAttributes redirectAttributes) {
        inventoryService.deleteProductMaterial(id);
        redirectAttributes.addFlashAttribute("success", "Material removed from BOM.");
        return "redirect:/inventory/finished-products/bom/" + productId;
    }

    private String calculateStatus(RawMaterial material) {
        int qty = material.getQuantity();

        // Default thresholds
        int outLimit = 5;
        int lowLimit = 10;

        // Try to fetch specific thresholds from Category
        if (material.getCategory() != null) {
            java.util.Optional<com.hotelcraft.model.MaterialCategory> category = categoryRepository
                    .findByName(material.getCategory());
            if (category.isPresent()) {
                com.hotelcraft.model.MaterialCategory cat = category.get();
                if (cat.getOutOfStockThreshold() != null)
                    outLimit = cat.getOutOfStockThreshold();
                if (cat.getLowStockThreshold() != null)
                    lowLimit = cat.getLowStockThreshold();
            }
        }

        if (qty < outLimit) {
            return "Out of Stock";
        } else if (qty <= lowLimit) {
            return "Low Stock";
        }
        return "In Stock";
    }
 
    private String saveProductImage(MultipartFile file) {
        try {
            String uploadDir = "uploads/products/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
 
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String filePath = dir.getAbsolutePath() + File.separator + fileName;
            file.transferTo(new File(filePath));
            return "/uploads/products/" + fileName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
