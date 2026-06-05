package com.hotelcraft.service;

import com.hotelcraft.repository.*;
import com.hotelcraft.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RawMaterialRepository rawMaterialRepository;

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    @Autowired
    private ProductMaterialRepository productMaterialRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private com.hotelcraft.repository.SupplyRequestRepository supplyRequestRepository;

    @Autowired
    private BookingItemRepository bookingItemRepository;

    // Product methods
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    public void saveProduct(Product product) {
        updateProductStatus(product);
        productRepository.save(product);
    }

    public Product findProductByName(String name) {
        return productRepository.findByNameIgnoreCase(name).orElse(null);
    }

    @Transactional
    public void updateProductStock(Long productId, Integer newQuantity, String remarks, String customerName) {
        Product product = getProductById(productId);
        if (product == null) return;

        int oldQuantity = (product.getQuantity() != null) ? product.getQuantity() : 0;
        int diff = newQuantity - oldQuantity;

        if (diff == 0) return;

        // 1. Update product stock FIRST
        product.setQuantity(newQuantity);
        updateProductStatus(product);
        productRepository.save(product);

        String type = diff > 0 ? "PRODUCTION" : "DELIVERY";
        if (remarks != null && (remarks.toLowerCase().contains("adjustment") || remarks.toLowerCase().contains("setup") || remarks.toLowerCase().contains("update"))) {
            type = "ADJUSTMENT";
        }

        // Record product movement
        InventoryMovement movement = new InventoryMovement(product, diff, type, remarks, customerName);
        inventoryMovementRepository.save(movement);

        // 2. If it's a PRODUCTION increase (diff > 0), check and deduct raw materials
        if (diff > 0) {
            List<ProductMaterial> materials = productMaterialRepository.findByProductId(product.getId());
            
            // Perform availability check with double precision
            for (ProductMaterial pm : materials) {
                RawMaterial rm = pm.getRawMaterial();
                double totalNeeded = pm.getQuantityRequired() * diff;
                
                if (rm.getQuantity() < totalNeeded) {
                    // This will trigger transaction rollback, undoing the product save above
                    throw new RuntimeException("Insufficient stock for " + rm.getName() + ". Required: " + (int)Math.ceil(totalNeeded) + ", Available: " + rm.getQuantity());
                }
            }

            // Perform deduction
            for (ProductMaterial pm : materials) {
                RawMaterial rm = pm.getRawMaterial();
                double totalNeeded = pm.getQuantityRequired() * diff;
                int roundedNeeded = (int)Math.round(totalNeeded);
                
                rm.setQuantity(rm.getQuantity() - roundedNeeded);
                updateRawMaterialStatus(rm);
                rawMaterialRepository.save(rm);
                checkAndNotifySupplier(rm);

                // Record consumption movement
                InventoryMovement rmMovement = new InventoryMovement(rm, -roundedNeeded, "CONSUMPTION", 
                    "Consumed for production of " + diff + " " + product.getName());
                inventoryMovementRepository.save(rmMovement);
            }
        }
    }

    private void updateProductStatus(Product product) {
        if (product.getQuantity() != null) {
            if (product.getQuantity() <= 0) {
                product.setStatus("Out of Stock");
            } else if (product.getQuantity() < 15) {
                product.setStatus("Low Stock");
            } else {
                product.setStatus("In Stock");
            }
        }
    }

    @Transactional
    public void deleteProduct(Long id) {
        // 1. Delete associated movements
        List<InventoryMovement> movements = inventoryMovementRepository.findByProductId(id, Sort.unsorted());
        inventoryMovementRepository.deleteAll(movements);

        // 2. Delete associated BOM items
        List<ProductMaterial> bomItems = productMaterialRepository.findByProductId(id);
        productMaterialRepository.deleteAll(bomItems);

        // 3. Delete associated booking items (Sales history)
        List<BookingItem> sales = bookingItemRepository.findAll().stream()
            .filter(item -> item.getProduct().getId().equals(id))
            .collect(java.util.stream.Collectors.toList());
        bookingItemRepository.deleteAll(sales);

        // 4. Finally delete the product
        productRepository.deleteById(id);
    }

    @Transactional
    public void mergeDuplicateProducts() {
        List<Product> all = productRepository.findAll().stream()
            .filter(p -> p.getName() != null && !p.getName().trim().isEmpty())
            .collect(java.util.stream.Collectors.toList());
        java.util.Map<String, List<Product>> grouped = all.stream()
            .collect(java.util.stream.Collectors.groupingBy(p -> p.getName().trim().toLowerCase()));

        for (List<Product> group : grouped.values()) {
            if (group.size() > 1) {
                // Keep the one with the highest ID (most recent)
                Product survivor = group.stream().max(java.util.Comparator.comparing(Product::getId)).get();
                int totalQuantity = survivor.getQuantity();

                for (Product duplicate : group) {
                    if (duplicate.getId().equals(survivor.getId())) continue;

                    totalQuantity += duplicate.getQuantity();

                    // Re-associate movements
                    List<InventoryMovement> movements = inventoryMovementRepository.findByProductId(duplicate.getId(), Sort.unsorted());
                    for (InventoryMovement m : movements) {
                        m.setProduct(survivor);
                        inventoryMovementRepository.save(m);
                    }

                    // Re-associate BOM items
                    List<ProductMaterial> bomItems = productMaterialRepository.findByProductId(duplicate.getId());
                    for (ProductMaterial pm : bomItems) {
                        pm.setProduct(survivor);
                        productMaterialRepository.save(pm);
                    }

                    productRepository.delete(duplicate);
                }

                survivor.setQuantity(totalQuantity);
                updateProductStatus(survivor);
                productRepository.save(survivor);
            }
        }
    }

    // Raw Material methods
    public List<RawMaterial> getAllRawMaterials() {
        return rawMaterialRepository.findAll();
    }

    public RawMaterial getRawMaterialById(Long id) {
        return rawMaterialRepository.findById(id).orElse(null);
    }

    public void saveRawMaterial(RawMaterial material) {
        updateRawMaterialStatus(material);
        rawMaterialRepository.save(material);
    }

    @Transactional
    public Product saveProductWithMaterials(Product product, List<Long> materialIds, List<Integer> quantities) {
        // Save the product first
        saveProduct(product);

        // Save materials
        if (materialIds != null && quantities != null) {
            for (int i = 0; i < materialIds.size(); i++) {
                RawMaterial material = getRawMaterialById(materialIds.get(i));
                if (material != null) {
                    ProductMaterial pm = new ProductMaterial(product, material, (double)quantities.get(i));
                    saveProductMaterial(pm);
                }
            }
        }
        return product;
    }

    @Transactional
    public void produceNewProduct(Product product, List<Long> materialIds, List<Integer> batchTotalQuantities, String customerName) {
        int initialQty = product.getQuantity() != null ? product.getQuantity() : 0;
        product.setQuantity(0); 
        
        // Save the product first
        saveProduct(product);

        // Save BOM by converting 'Batch Total' to 'Per Unit'
        if (materialIds != null && batchTotalQuantities != null && initialQty > 0) {
            for (int i = 0; i < materialIds.size(); i++) {
                RawMaterial material = getRawMaterialById(materialIds.get(i));
                if (material != null) {
                    // Calculate: Total Used / Total Produced = Per Unit Requirement
                    double perUnit = (double) batchTotalQuantities.get(i) / initialQty;
                    ProductMaterial pm = new ProductMaterial(product, material, perUnit);
                    saveProductMaterial(pm);
                }
            }
        }
        
        if (initialQty > 0) {
            updateProductStock(product.getId(), initialQty, "Initial production setup", customerName);
        }
    }

    public RawMaterial findRawMaterialByName(String name) {
        return rawMaterialRepository.findByNameIgnoreCase(name).orElse(null);
    }

    @Transactional
    public void updateRawMaterialStock(Long materialId, Integer newQuantity, String remarks) {
        RawMaterial material = getRawMaterialById(materialId);
        if (material == null) return;

        int oldQuantity = material.getQuantity();
        int diff = newQuantity - oldQuantity;

        if (diff == 0) return;

        // Prevent negative quantity on manual updates as well
        material.setQuantity(Math.max(0, newQuantity));
        updateRawMaterialStatus(material);
        rawMaterialRepository.save(material);
        checkAndNotifySupplier(material);

        String type = diff > 0 ? "RESTOCK" : "ADJUSTMENT";
        
        // Record movement
        InventoryMovement movement = new InventoryMovement(material, diff, type, remarks);
        inventoryMovementRepository.save(movement);
    }

    private void updateRawMaterialStatus(RawMaterial material) {
        if (material.getQuantity() != null) {
            if (material.getQuantity() <= 0) {
                material.setStatus("Out of Stock");
            } else if (material.getQuantity() < 25) { // Lowered threshold for alert demonstration
                material.setStatus("Low Stock");
            } else {
                material.setStatus("In Stock");
            }
        }
    }

    private void checkAndNotifySupplier(RawMaterial material) {
        if ("Low Stock".equals(material.getStatus()) || "Out of Stock".equals(material.getStatus())) {
            String title = "Urgent: " + material.getName() + " is " + material.getStatus();
            String message = "Current stock level: " + material.getQuantity() + " (Supplier: " + material.getSupplier() + "). Please arrange for restocking immediately.";
            String type = "Out of Stock".equals(material.getStatus()) ? "RED" : "AMBER";
            
            // Avoid duplicate recent alerts (within last 1 hour)
            List<Notification> recent = notificationRepository.findAllByOrderByTimestampDesc();
            boolean alreadyNotified = recent.stream()
                .anyMatch(n -> n.getTitle().contains(material.getName()) && 
                               n.getTimestamp().isAfter(LocalDateTime.now().minusHours(1)));
            
            if (!alreadyNotified) {
                Notification note = new Notification(title, message, type);
                notificationRepository.save(note);
            }
        }
    }

    // BOM Management
    public List<ProductMaterial> getProductBOM(Long productId) {
        return productMaterialRepository.findByProductId(productId);
    }

    @Transactional
    public void saveProductMaterial(ProductMaterial pm) {
        productMaterialRepository.save(pm);
    }

    @Transactional
    public void deleteProductMaterial(Long id) {
        productMaterialRepository.deleteById(id);
    }

    // History
    public List<InventoryMovement> getProductHistory(Long productId) {
        return inventoryMovementRepository.findByProductId(productId, Sort.by(Sort.Direction.DESC, "timestamp"));
    }

    public List<InventoryMovement> getRawMaterialHistory(Long materialId) {
        return inventoryMovementRepository.findByRawMaterialId(materialId, Sort.by(Sort.Direction.DESC, "timestamp"));
    }

    @Transactional
    public void deleteRawMaterial(Long id) {
        // 1. Delete associated movements (History)
        List<InventoryMovement> movements = inventoryMovementRepository.findByRawMaterialId(id, Sort.unsorted());
        inventoryMovementRepository.deleteAll(movements);

        // 2. Delete associated supply requests to avoid FK constraint violations
        supplyRequestRepository.deleteByMaterialId(id);
        
        // 3. Also delete associations with products (BOM items)
        List<ProductMaterial> bomItems = productMaterialRepository.findAll().stream()
            .filter(pm -> pm.getRawMaterial().getId().equals(id))
            .collect(java.util.stream.Collectors.toList());
        productMaterialRepository.deleteAll(bomItems);

        // 4. Finally delete the raw material
        rawMaterialRepository.deleteById(id);
    }

    @Transactional
    public void mergeDuplicateRawMaterials() {
        List<RawMaterial> all = rawMaterialRepository.findAll();
        java.util.Map<String, List<RawMaterial>> grouped = all.stream()
            .collect(java.util.stream.Collectors.groupingBy(m -> m.getName().toLowerCase()));

        for (List<RawMaterial> group : grouped.values()) {
            if (group.size() > 1) {
                RawMaterial survivor = group.stream().max(java.util.Comparator.comparing(RawMaterial::getId)).get();
                int totalQuantity = survivor.getQuantity();

                for (RawMaterial duplicate : group) {
                    if (duplicate.getId().equals(survivor.getId())) continue;

                    totalQuantity += duplicate.getQuantity();

                    // Re-associate movements
                    List<InventoryMovement> movements = inventoryMovementRepository.findByRawMaterialId(duplicate.getId(), Sort.unsorted());
                    for (InventoryMovement m : movements) {
                        m.setRawMaterial(survivor);
                        inventoryMovementRepository.save(m);
                    }

                    // Re-associate BOM items where this was a material
                    List<ProductMaterial> bomUses = productMaterialRepository.findAll().stream()
                        .filter(pm -> pm.getRawMaterial().getId().equals(duplicate.getId()))
                        .collect(java.util.stream.Collectors.toList());
                    for (ProductMaterial pm : bomUses) {
                        pm.setRawMaterial(survivor);
                        productMaterialRepository.save(pm);
                    }

                    rawMaterialRepository.delete(duplicate);
                }

                survivor.setQuantity(totalQuantity);
                updateRawMaterialStatus(survivor);
                rawMaterialRepository.save(survivor);
            }
        }
    }

    @Transactional
    public void cleanupUnusedMaterials() {
        List<RawMaterial> all = rawMaterialRepository.findAll();
        for (RawMaterial material : all) {
            // Criteria: 0 quantity AND (no movements recorded)
            List<InventoryMovement> movements = getRawMaterialHistory(material.getId());
            if (material.getQuantity() <= 0 && (movements == null || movements.isEmpty())) {
                // Also check if it's not used in any BOM
                List<ProductMaterial> uses = productMaterialRepository.findAll().stream()
                    .filter(pm -> pm.getRawMaterial().getId().equals(material.getId()))
                    .collect(java.util.stream.Collectors.toList());
                
                if (uses.isEmpty()) {
                    rawMaterialRepository.delete(material);
                }
            }
        }
    }

    @Transactional
    public void cleanupNegativeStocks() {
        List<RawMaterial> all = rawMaterialRepository.findAll();
        for (RawMaterial material : all) {
            if (material.getQuantity() != null && material.getQuantity() < 0) {
                material.setQuantity(0);
                updateRawMaterialStatus(material);
                rawMaterialRepository.save(material);
            }
        }
    }
}
