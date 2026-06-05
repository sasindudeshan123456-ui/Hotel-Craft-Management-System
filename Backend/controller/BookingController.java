package com.hotelcraft.controller;

import com.hotelcraft.model.Booking;
import com.hotelcraft.model.Product;
import com.hotelcraft.model.User;
import com.hotelcraft.service.BookingService;
import com.hotelcraft.service.InventoryService;
import com.hotelcraft.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;

import java.security.Principal;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @Autowired
    private InventoryService inventoryService;

    @GetMapping
    public String myBookings(Principal principal, Model model, RedirectAttributes ra) {
        if (principal == null) {
            ra.addFlashAttribute("error", "Please login to view your bookings.");
            return "redirect:/login";
        }
        
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        model.addAttribute("bookings", bookingService.getBookingsByUser(user));
        return "user/bookings/list";
    }

    @GetMapping("/edit/{id}")
    public String editBooking(@PathVariable("id") Long id, Principal principal, Model model, RedirectAttributes ra) {
        if (principal == null) {
            ra.addFlashAttribute("error", "Please login to manage bookings.");
            return "redirect:/login";
        }
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        Booking booking = bookingService.getBookingById(id);
        
        if (booking != null && (booking.getUser().equals(user) || user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN")))) {
            if ("Pending".equals(booking.getStatus())) {
                model.addAttribute("booking", booking);
                model.addAttribute("bookingItems", bookingService.getItemsByBooking(booking));
                model.addAttribute("products", inventoryService.getAllProducts());
                return "user/bookings/edit";
            }
        }
        return "redirect:/bookings";
    }

    @PostMapping("/{id}/add-items")
    public String addItems(@PathVariable("id") Long id,
            @RequestParam("productId") Long productId,
            @RequestParam("quantity") int quantity,
            Principal principal,
            RedirectAttributes ra) {
        Booking booking = bookingService.getBookingById(id);
        if (booking != null && principal != null && booking.getUser().getEmail().equals(principal.getName())) {
            if ("Pending".equals(booking.getStatus())) {
                Product product = inventoryService.getProductById(productId);
                if (product != null && quantity > 0) {
                    bookingService.addItemToBooking(booking, product, quantity);
                    ra.addFlashAttribute("success", "Item added to booking!");
                }
            }
        }
        return "redirect:/bookings/edit/" + id;
    }

    @PostMapping("/{id}/remove-item/{itemId}")
    public String removeItem(@PathVariable("id") Long id,
            @PathVariable("itemId") Long itemId,
            Principal principal,
            RedirectAttributes ra) {
        Booking booking = bookingService.getBookingById(id);
        if (booking != null && principal != null && booking.getUser().getEmail().equals(principal.getName())
                && "Pending".equals(booking.getStatus())) {
            bookingService.removeItemFromBooking(booking, itemId);
            ra.addFlashAttribute("success", "Item removed.");
        }
        return "redirect:/bookings/edit/" + id;
    }

    @PostMapping("/{id}/update-item/{itemId}")
    public String updateItemQty(@PathVariable("id") Long id,
            @PathVariable("itemId") Long itemId,
            @RequestParam("quantity") int quantity,
            Principal principal,
            RedirectAttributes ra) {
        Booking booking = bookingService.getBookingById(id);
        if (booking != null && principal != null && booking.getUser().getEmail().equals(principal.getName())
                && "Pending".equals(booking.getStatus())) {
            if (quantity <= 0) {
                bookingService.removeItemFromBooking(booking, itemId);
            } else {
                bookingService.updateItemQuantity(booking, itemId, quantity);
            }
        }
        return "redirect:/bookings/edit/" + id;
    }

    @PostMapping("/edit/{id}")
    public String updateBooking(@PathVariable("id") Long id, @ModelAttribute Booking bookingData, Principal principal,
            RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        Booking booking = bookingService.getBookingById(id);
        
        if (booking != null && (booking.getUser().equals(user) || user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN")))) {
            if ("Pending".equals(booking.getStatus())) {
                booking.setCheckIn(bookingData.getCheckIn());
                booking.setCheckOut(bookingData.getCheckOut());
                booking.setCustomerName(bookingData.getCustomerName());
                bookingService.saveBooking(booking);
                ra.addFlashAttribute("success", "Booking updated successfully.");
            }
        }
        return "redirect:/bookings";
    }

    @PostMapping("/delete/{id}")
    public String deleteBooking(@PathVariable("id") Long id, Principal principal,
            RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElseThrow();
        Booking booking = bookingService.getBookingById(id);
        
        if (booking != null && (booking.getUser().equals(user) || user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN")))) {
            if ("Pending".equals(booking.getStatus())) {
                bookingService.deleteBooking(id);
                ra.addFlashAttribute("success", "Booking deleted successfully.");
            } else {
                ra.addFlashAttribute("error", "Only pending bookings can be deleted.");
            }
        }
        return "redirect:/bookings";
    }
}
