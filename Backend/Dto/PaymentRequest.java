package com.hotelcraft.dto;

import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.Pattern;

public class PaymentRequest {
    private String paymentMethod;
    private String cardHolderName;
    @Pattern(regexp = "^$|^\\d{4} \\d{4} \\d{4} \\d{4}$", message = "Card number must be exactly 16 digits (formatted with spaces)")
    private String cardNumber;

    @Pattern(regexp = "^$|^(0[1-9]|1[0-2])\\/\\d{2}$", message = "Expiry date must be in MM/YY format with a valid month (01-12)")
    private String expiryDate;
    private String cvv;
    private MultipartFile bankSlip;
    private Double amount;
    private Boolean deliveryNeeded;
    private String deliveryAddress;
    private Double deliveryCharges;
    private Double discountAmount;
    private Double taxAmount;
    private String cartJson;

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

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public MultipartFile getBankSlip() {
        return bankSlip;
    }

    public void setBankSlip(MultipartFile bankSlip) {
        this.bankSlip = bankSlip;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
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

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Double getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(Double taxAmount) {
        this.taxAmount = taxAmount;
    }

    public String getCartJson() {
        return cartJson;
    }

    public void setCartJson(String cartJson) {
        this.cartJson = cartJson;
    }
}
