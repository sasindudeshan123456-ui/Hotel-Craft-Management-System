package com.hotelcraft.controller;

import com.hotelcraft.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/bookings")
@PreAuthorize("hasAuthority('BOOKING_READ_ALL')")
public class AdminBookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping
    public String listAllBookings(Model model) {
        model.addAttribute("bookings", bookingService.getAllBookings());
        return "admin/bookings/list";
    }

    @PostMapping("/manage/{id}")
    public String manageBooking(@PathVariable("id") Long id, @RequestParam("status") String status,
            RedirectAttributes redirectAttributes) {
        bookingService.updateBookingStatus(id, status);
        redirectAttributes.addFlashAttribute("success", "Booking status updated successfully.");
        return "redirect:/admin/bookings";
    }

    @PostMapping("/approve/{id}")
    public String approveBooking(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        bookingService.updateBookingStatus(id, "Approved");
        redirectAttributes.addFlashAttribute("success", "Booking approved successfully.");
        return "redirect:/admin/bookings";
    }

    @GetMapping("/edit/{id}")
    public String editBookingForm(@PathVariable("id") Long id, Model model) {
        com.hotelcraft.model.Booking booking = bookingService.getBookingById(id);
        if (booking == null) return "redirect:/admin/bookings";
        model.addAttribute("booking", booking);
        return "admin/bookings/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateBooking(@PathVariable("id") Long id, 
            @RequestParam("status") String status,
            @RequestParam("totalAmount") Double totalAmount,
            @RequestParam(value = "deliveryDate", required = false) String deliveryDate,
            @RequestParam(value = "totalQuantity", required = false) Integer totalQuantity,
            @RequestParam(value = "deliveryAddress", required = false) String deliveryAddress,
            RedirectAttributes redirectAttributes) {
        com.hotelcraft.model.Booking booking = bookingService.getBookingById(id);
        if (booking != null) {
            booking.setStatus(status);
            booking.setTotalAmount(totalAmount);
            booking.setTotalQuantity(totalQuantity);
            booking.setDeliveryAddress(deliveryAddress);
            
            if (deliveryDate != null && !deliveryDate.isEmpty()) {
                booking.setDeliveryDate(java.time.LocalDate.parse(deliveryDate));
            }
            
            bookingService.saveBooking(booking);
            redirectAttributes.addFlashAttribute("success", "Booking updated successfully.");
        }
        return "redirect:/admin/bookings";
    }

    @PostMapping("/delete/{id}")
    public String deleteBooking(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        bookingService.deleteBooking(id);
        redirectAttributes.addFlashAttribute("success", "Booking deleted successfully.");
        return "redirect:/admin/bookings";
    }
}
