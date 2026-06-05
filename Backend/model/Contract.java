package com.hotelcraft.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    private Double value;
    private LocalDate expiresAt;

    @Column(nullable = false)
    private String status; // PENDING, APPROVED, ACTIVE, EXPIRED

    public Contract() {
    }

    public Contract(String title, Supplier supplier, Double value, LocalDate expiresAt, String status) {
        this.title = title;
        this.supplier = supplier;
        this.value = value;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDate expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
