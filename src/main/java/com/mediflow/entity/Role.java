package com.mediflow.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {

    public static final String ADMIN = "ADMINISTRATOR";
    public static final String PHARMACIST = "PHARMACIST";
    public static final String STORE_KEEPER = "STORE_KEEPER";
    public static final String PROCUREMENT_OFFICER = "PROCUREMENT_OFFICER";
    public static final String CASHIER = "CASHIER";
    public static final String CRO = "CUSTOMER_RELATIONS_OFFICER";
    public static final String FINANCE_MANAGER = "FINANCE_MANAGER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    public Role() {
    }

    public Role(String name) {
        this.name = name;
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
}
