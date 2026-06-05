package com.hotelcraft.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_material_id")
    private RawMaterial rawMaterial;

    @Column(nullable = false)
    private Integer changeAmount;

    @Column(nullable = false)
    private String type; // PRODUCTION, CONSUMPTION, RESTOCK, ADJUSTMENT, INITIAL_STOCK, DELIVERY

    private String remarks;
    
    private String customerName;

    private LocalDateTime timestamp = LocalDateTime.now();

    public InventoryMovement() {
    }

    public InventoryMovement(Product product, Integer changeAmount, String type, String remarks, String customerName) {
        this.product = product;
        this.changeAmount = changeAmount;
        this.type = type;
        this.remarks = remarks;
        this.customerName = customerName;
    }

    public InventoryMovement(RawMaterial rawMaterial, Integer changeAmount, String type, String remarks) {
        this.rawMaterial = rawMaterial;
        this.changeAmount = changeAmount;
        this.type = type;
        this.remarks = remarks;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public RawMaterial getRawMaterial() {
        return rawMaterial;
    }

    public void setRawMaterial(RawMaterial rawMaterial) {
        this.rawMaterial = rawMaterial;
    }

    public Integer getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(Integer changeAmount) {
        this.changeAmount = changeAmount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
