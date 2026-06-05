package com.hotelcraft.repository;

import com.hotelcraft.model.Product;
import com.hotelcraft.model.ProductMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductMaterialRepository extends JpaRepository<ProductMaterial, Long> {
    List<ProductMaterial> findByProduct(Product product);
    List<ProductMaterial> findByProductId(Long productId);
}
