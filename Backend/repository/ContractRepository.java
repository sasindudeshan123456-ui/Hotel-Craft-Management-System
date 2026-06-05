package com.hotelcraft.repository;

import com.hotelcraft.model.Contract;
import com.hotelcraft.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    List<Contract> findBySupplier(Supplier supplier);
}
