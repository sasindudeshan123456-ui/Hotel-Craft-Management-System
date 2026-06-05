package com.hotelcraft.repository;

import com.hotelcraft.model.SupplyRequest;
import com.hotelcraft.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupplyRequestRepository extends JpaRepository<SupplyRequest, Long> {
    List<SupplyRequest> findBySupplier(Supplier supplier);
    List<SupplyRequest> findByStatus(String status);
    List<SupplyRequest> findBySupplierAndStatus(Supplier supplier, String status);
    void deleteByMaterialId(Long materialId);
}
