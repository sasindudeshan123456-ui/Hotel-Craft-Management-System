package com.hotelcraft.repository;

import com.hotelcraft.model.SupplierOrder;
import com.hotelcraft.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SupplierOrderRepository extends JpaRepository<SupplierOrder, String> {
    List<SupplierOrder> findBySupplier(Supplier supplier);
}
