package com.hotelcraft.model;

import jakarta.persistence.*;

@Entity
@Table(name = "material_categories")
public class MaterialCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private Integer lowStockThreshold = 10;
    private Integer outOfStockThreshold = 5;

    public MaterialCategory() {
    }

    public MaterialCategory(String name) {
        this.name = name;
    }

    public MaterialCategory(String name, Integer lowStockThreshold, Integer outOfStockThreshold) {
        this.name = name;
        this.lowStockThreshold = lowStockThreshold;
        this.outOfStockThreshold = outOfStockThreshold;
    }

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

    public Integer getLowStockThreshold() {
        return lowStockThreshold != null ? lowStockThreshold : 10;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public Integer getOutOfStockThreshold() {
        return outOfStockThreshold != null ? outOfStockThreshold : 5;
    }

    public void setOutOfStockThreshold(Integer outOfStockThreshold) {
        this.outOfStockThreshold = outOfStockThreshold;
    }
}
