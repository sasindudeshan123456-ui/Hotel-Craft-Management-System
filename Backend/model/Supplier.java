package com.hotelcraft.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String category;

    @Column(nullable = false)
    private String status; // ACTIVE, REVIEW

    private Double onTimeRate;
    private Integer orderCount;
    private Double rating;
    private String initials;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL)
    private List<Contract> contracts;

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL)
    private List<SupplierOrder> orders;

    public Supplier() {
    }

    public Supplier(String name, String category, String status, Double onTimeRate, Integer orderCount, Double rating,
            String initials) {
        this.name = name;
        this.category = category;
        this.status = status;
        this.onTimeRate = onTimeRate;
        this.orderCount = orderCount;
        this.rating = rating;
        this.initials = initials;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getOnTimeRate() {
        return onTimeRate;
    }

    public void setOnTimeRate(Double onTimeRate) {
        this.onTimeRate = onTimeRate;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getInitials() {
        return initials;
    }

    public void setInitials(String initials) {
        this.initials = initials;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Contract> getContracts() {
        return contracts;
    }

    public void setContracts(List<Contract> contracts) {
        this.contracts = contracts;
    }

    public List<SupplierOrder> getOrders() {
        return orders;
    }

    public void setOrders(List<SupplierOrder> orders) {
        this.orders = orders;
    }
}
