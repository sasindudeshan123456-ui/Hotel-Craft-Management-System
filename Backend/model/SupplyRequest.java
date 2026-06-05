package com.hotelcraft.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "supply_requests")
public class SupplyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "material_id")
    private RawMaterial material;

    private Integer offeredQuantity; // Quantity the supplier offers
    private Integer approvedQuantity; // Quantity admin decides to receive
    private Double pricePerUnit; // Price per unit proposed by supplier

    private String status; // PENDING, APPROVED (Deal accepted), REJECTED, RECEIVED (In Stock)

    private LocalDateTime createdAt = LocalDateTime.now();

    public SupplyRequest() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public RawMaterial getMaterial() {
        return material;
    }

    public void setMaterial(RawMaterial material) {
        this.material = material;
    }

    public Integer getOfferedQuantity() {
        return offeredQuantity;
    }

    public void setOfferedQuantity(Integer offeredQuantity) {
        this.offeredQuantity = offeredQuantity;
    }

    public Integer getApprovedQuantity() {
        return approvedQuantity;
    }

    public void setApprovedQuantity(Integer approvedQuantity) {
        this.approvedQuantity = approvedQuantity;
    }

    public Double getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(Double pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
