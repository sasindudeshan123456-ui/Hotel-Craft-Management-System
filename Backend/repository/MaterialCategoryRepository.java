package com.hotelcraft.repository;

import com.hotelcraft.model.MaterialCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MaterialCategoryRepository extends JpaRepository<MaterialCategory, Long> {
    Optional<MaterialCategory> findByName(String name);
}
