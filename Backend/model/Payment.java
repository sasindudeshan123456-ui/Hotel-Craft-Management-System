package com.hotelcraft.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String paymentMethod; // Card, COD, Bank Transfer

    private String cardHolderName;
    private String cardNumber;
    private String expiryDate;
    private String bankSlipPath;

    @Column(nullable = false)
    private Boolean deliveryNeeded = false;

    private String deliveryAddress;
    private Double deliveryCharges = 0.0;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String status; // Pending, Completed, Failed, Refunded

    private LocalDateTime paymentDate = LocalDateTime.now();

    public Payment() {
    }

    public Payment(Booking booking, Double amount, String paymentMethod, String transactionId, String status) {
        this.booking = booking;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getBankSlipPath() {
        return bankSlipPath;
    }

    public void setBankSlipPath(String bankSlipPath) {
        this.bankSlipPath = bankSlipPath;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Boolean getDeliveryNeeded() {
        return deliveryNeeded;
    }

    public void setDeliveryNeeded(Boolean deliveryNeeded) {
        this.deliveryNeeded = deliveryNeeded;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public Double getDeliveryCharges() {
        return deliveryCharges;
    }

    public void setDeliveryCharges(Double deliveryCharges) {
        this.deliveryCharges = deliveryCharges;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }
}
