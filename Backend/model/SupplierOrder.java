package com.hotelcraft.model;

import jakarta.persistence.*;

@Entity
@Table(name = "supplier_orders")
public class SupplierOrder {

    @Id
    private String id; // e.g. ORD-841

    @Column(nullable = false)
    private String item;

    private String quantity;

    @Column(nullable = false)
    private String status; // NEW, PROCESSING, DELIVERED

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    public SupplierOrder() {
    }

    public SupplierOrder(String id, String item, String quantity, String status) {
        this.id = id;
        this.item = item;
        this.quantity = quantity;
        this.status = status;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }
}
