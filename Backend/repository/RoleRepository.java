package com.hotelcraft.repository;

import com.hotelcraft.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    void deleteByName(String name);
}

