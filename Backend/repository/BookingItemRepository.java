package com.hotelcraft.repository;

import com.hotelcraft.model.Booking;
import com.hotelcraft.model.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {
    List<BookingItem> findByBooking(Booking booking);
}

