package com.hotelcraft.controller;

import com.hotelcraft.dto.PaymentRequest;
import com.hotelcraft.model.Booking;
import com.hotelcraft.model.Payment;
import com.hotelcraft.model.Product;
import com.hotelcraft.model.User;
import com.hotelcraft.service.BookingService;
import com.hotelcraft.service.InventoryService;
import com.hotelcraft.service.PaymentService;
import com.hotelcraft.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/store")
public class StoreController {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("products", inventoryService.getAllProducts());
        return "store/items_fixed";
    }

    @GetMapping("/cart")
    public String cart(Model model) {
        return "store/cart";
    }

    @GetMapping("/payment")
    public String payment(java.security.Principal principal, Model model) {
        if (principal != null) {
            userService.findByEmail(principal.getName()).ifPresent(user -> {
                model.addAttribute("user", user);
            });
        }
        return "store/payment";
    }

    @PostMapping("/checkout")
    public String checkout() {
        return "redirect:/store/payment";
    }

    @PostMapping("/payment")
    public String processPayment(@jakarta.validation.Valid PaymentRequest request,
            org.springframework.validation.BindingResult bindingResult,
            java.security.Principal principal,
            Model model,
            RedirectAttributes ra) {

        if (principal == null) {
            ra.addFlashAttribute("error", "Please login to complete your purchase.");
            return "redirect:/login";
        }
        
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("paymentErrors", bindingResult.getAllErrors());
            return "store/payment";
        }

        // Create booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCustomerName(user.getFullName());
        booking.setRoomType("Store Purchase");
        booking.setCheckIn(java.time.LocalDateTime.now());
        booking.setCheckOut(java.time.LocalDateTime.now().plusDays(1));
        booking.setStatus("Pending");
        booking.setTotalAmount(request.getAmount() != null ? request.getAmount() : 100.0);
        bookingService.saveBooking(booking);

        // Create payment
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionId("TXN-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        payment.setStatus("Completed");
        payment.setDeliveryNeeded(request.getDeliveryNeeded() != null ? request.getDeliveryNeeded() : false);
        payment.setDeliveryAddress(request.getDeliveryAddress());
        payment.setDeliveryCharges(request.getDeliveryCharges() != null ? request.getDeliveryCharges() : 0.0);

        if (payment.getDeliveryNeeded()) {
            booking.setTotalAmount(booking.getTotalAmount() + payment.getDeliveryCharges());
            booking.setDeliveryAddress(payment.getDeliveryAddress());
            payment.setAmount(booking.getTotalAmount());
            bookingService.saveBooking(booking); // Update amount and address
        }

        if ("Card".equals(request.getPaymentMethod())) {
            payment.setCardHolderName(request.getCardHolderName());
            payment.setCardNumber(request.getCardNumber());
            payment.setExpiryDate(request.getExpiryDate());
        } else if ("Bank Transfer".equals(request.getPaymentMethod()) && request.getBankSlip() != null) {
            try {
                String uploadDir = "uploads/bank-slips/";
                java.io.File dir = new java.io.File(uploadDir);
                if (!dir.exists())
                    dir.mkdirs();

                String fileName = System.currentTimeMillis() + "_" + request.getBankSlip().getOriginalFilename();
                request.getBankSlip()
                        .transferTo(new java.io.File(dir.getAbsolutePath() + java.io.File.separator + fileName));
                payment.setBankSlipPath(uploadDir + fileName);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }

        paymentService.processPayment(payment);

        // Persist cart items as BookingItems
        String cartJson = request.getCartJson();
        if (cartJson != null && !cartJson.isBlank() && !cartJson.equals("[]")) {
            try {
                cartJson = cartJson.trim();
                if (cartJson.startsWith("["))
                    cartJson = cartJson.substring(1, cartJson.length() - 1);
                for (String entry : cartJson.split("\\},\\{")) {
                    entry = entry.replace("{", "").replace("}", "");
                    Long productId = null;
                    int qty = 1;
                    for (String kv : entry.split(",")) {
                        String[] parts = kv.split(":");
                        if (parts.length == 2) {
                            String key = parts[0].trim().replace("\"", "");
                            String val = parts[1].trim().replace("\"", "");
                            if ("id".equals(key))
                                productId = Long.parseLong(val);
                            if ("quantity".equals(key))
                                qty = Integer.parseInt(val);
                        }
                    }
                    if (productId != null) {
                        Product product = inventoryService.getProductById(productId);
                        if (product != null) {
                            bookingService.addItemToBooking(booking, product, qty);
                            
                            // Automatically decrease stock of Finished Product
                            int newQty = (product.getQuantity() != null ? product.getQuantity() : 0) - qty;
                            inventoryService.updateProductStock(productId, newQty, "Storefront Purchase: Order #" + booking.getId(), user.getFullName());
                        }
                    }
                }
                // Set final total and discount from request AFTER adding items to avoid recalculation overwrite
                if (request.getAmount() != null) booking.setTotalAmount(request.getAmount());
                if (request.getDiscountAmount() != null) booking.setDiscount(request.getDiscountAmount());
                bookingService.saveBooking(booking);
            } catch (Exception ignored) {
            }
        }

        ra.addFlashAttribute("success", "Purchase successful! Your order has been placed.");
        return "redirect:/store";
    }

    @PostMapping("/update-price/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CART_DISCOUNT')")
    public String updatePrice(@PathVariable("id") Long id,
            @RequestParam("price") Double price,
            @RequestParam("discount") Double discount,
            RedirectAttributes ra) {
        com.hotelcraft.model.Product product = inventoryService.getProductById(id);
        if (product != null) {
            product.setPrice(price);
            product.setDiscount(discount);
            inventoryService.saveProduct(product);
            ra.addFlashAttribute("success", "Price and discount updated for " + product.getName());
        }
        return "redirect:/store";
    }
}

