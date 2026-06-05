package com.hotelcraft.repository;

import com.hotelcraft.model.Booking;
import com.hotelcraft.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUser(User user);
    
    List<Booking> findByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
    
    List<Booking> findByStatusNot(String status);
}
