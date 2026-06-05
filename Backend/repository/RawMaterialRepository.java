package com.hotelcraft.repository;

import com.hotelcraft.model.RawMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawMaterialRepository extends JpaRepository<RawMaterial, Long> {
    java.util.Optional<RawMaterial> findByNameIgnoreCase(String name);
}
