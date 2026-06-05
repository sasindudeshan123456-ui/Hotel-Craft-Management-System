package com.hotelcraft.service;

import com.hotelcraft.model.Booking;
import com.hotelcraft.model.BookingItem;
import com.hotelcraft.model.Product;
import com.hotelcraft.model.User;
import com.hotelcraft.repository.BookingItemRepository;
import com.hotelcraft.repository.BookingRepository;
import com.hotelcraft.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingItemRepository bookingItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public List<Booking> getBookingsByUser(User user) {
        return bookingRepository.findByUser(user);
    }

    public List<Booking> getPendingBookingsByUser(User user) {
        return bookingRepository.findByUser(user).stream()
                .filter(b -> "Pending".equals(b.getStatus()))
                .toList();
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id).orElse(null);
    }

    public void saveBooking(Booking booking) {
        bookingRepository.save(booking);
    }

    public void deleteBooking(Long id) {
        // Delete booking items first to avoid FK constraint violations
        Booking booking = getBookingById(id);
        if (booking != null) {
            // Delete associated booking items
            bookingItemRepository.deleteAll(bookingItemRepository.findByBooking(booking));

            // Delete associated payment if exists
            paymentRepository.findByBooking(booking).ifPresent(paymentRepository::delete);
        }
        bookingRepository.deleteById(id);
    }

    public void updateBookingStatus(Long id, String status) {
        Booking booking = getBookingById(id);
        if (booking != null) {
            booking.setStatus(status);
            if ("Approved".equals(status)) {
                booking.setApprovalDate(java.time.LocalDateTime.now());
            }
            bookingRepository.save(booking);
        }
    }

    public List<BookingItem> getItemsByBooking(Booking booking) {
        return bookingItemRepository.findByBooking(booking);
    }

    public void addItemToBooking(Booking booking, Product product, int quantity) {
        double unitPrice = product.getDiscount() != null && product.getDiscount() > 0
                ? product.getPrice() * (1 - product.getDiscount() / 100)
                : product.getPrice();

        List<BookingItem> existing = bookingItemRepository.findByBooking(booking);
        BookingItem existingItem = existing.stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst().orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            bookingItemRepository.save(existingItem);
        } else {
            BookingItem item = new BookingItem(booking, product, quantity, unitPrice);
            bookingItemRepository.save(item);
        }

        recalculateTotal(booking);
    }

    public void removeItemFromBooking(Booking booking, Long itemId) {
        bookingItemRepository.findById(itemId).ifPresent(item -> {
            if (item.getBooking().getId().equals(booking.getId())) {
                bookingItemRepository.delete(item);
            }
        });
        recalculateTotal(booking);
    }

    public void updateItemQuantity(Booking booking, Long itemId, int quantity) {
        bookingItemRepository.findById(itemId).ifPresent(item -> {
            if (item.getBooking().getId().equals(booking.getId())) {
                item.setQuantity(quantity);
                bookingItemRepository.save(item);
            }
        });
        recalculateTotal(booking);
    }

    private void recalculateTotal(Booking booking) {
        List<BookingItem> items = bookingItemRepository.findByBooking(booking);
        double subtotal = items.stream().mapToDouble(BookingItem::getSubtotal).sum();
        
        // Tiered Discounts (matching frontend defaults/user expectation)
        double discountPct = 0;
        if (subtotal >= 1000) {
            discountPct = 0.10; // Tier 2
        } else if (subtotal >= 500) {
            discountPct = 0.06; // Tier 1
        }
        
        double discountAmount = subtotal * discountPct;
        double subtotalAfterDiscount = subtotal - discountAmount;
        
        // 5% Tax on discounted subtotal
        double taxAmount = subtotalAfterDiscount * 0.05;
        
        double finalTotal = subtotalAfterDiscount + taxAmount;
        
        booking.setDiscount(discountAmount);
        booking.setTotalAmount(finalTotal);
        
        // Update total quantity
        int totalQty = items.stream().mapToInt(BookingItem::getQuantity).sum();
        booking.setTotalQuantity(totalQty);
        
        bookingRepository.save(booking);
    }
}
