package com.hotelcraft.service;

import com.hotelcraft.model.Booking;
import com.hotelcraft.model.Payment;
import com.hotelcraft.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> getPaymentByBooking(Booking booking) {
        return paymentRepository.findByBooking(booking);
    }

    public void processPayment(Payment payment) {
        // Here you would integrate with a payment gateway (e.g., Stripe, PayPal)
        // For now, we simulate a successful transaction
        payment.setStatus("Completed");
        paymentRepository.save(payment);
    }

    public void savePayment(Payment payment) {
        paymentRepository.save(payment);
    }
}
