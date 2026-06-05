package com.hotelcraft.repository;

import com.hotelcraft.model.InventoryMovement;
import com.hotelcraft.model.Product;
import com.hotelcraft.model.RawMaterial;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    List<InventoryMovement> findByProduct(Product product, Sort sort);
    List<InventoryMovement> findByProductId(Long productId, Sort sort);
    List<InventoryMovement> findByRawMaterial(RawMaterial material, Sort sort);
    List<InventoryMovement> findByRawMaterialId(Long materialId, Sort sort);
}
