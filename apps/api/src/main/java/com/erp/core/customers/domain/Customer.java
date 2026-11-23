package com.erp.core.customers.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers", schema = "erp")
public class Customer {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", nullable = false, length = 16)
    private PersonType personType;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "trade_name", length = 160)
    private String tradeName;

    @Column(nullable = false, length = 14)
    private String document;

    @Column(name = "state_registration", length = 32)
    private String stateRegistration;

    @Column(length = 254)
    private String email;

    @Column(length = 32)
    private String phone;

    @Embedded
    private CustomerAddress address;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Customer() { }

    public Customer(PersonType personType, String name, String tradeName, String document,
                    String stateRegistration, String email, String phone, CustomerAddress address) {
        this.id = UUID.randomUUID();
        this.personType = personType;
        this.name = name;
        this.tradeName = tradeName;
        this.document = document;
        this.stateRegistration = stateRegistration;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.active = true;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public PersonType getPersonType() { return personType; }
    public String getName() { return name; }
    public String getTradeName() { return tradeName; }
    public String getDocument() { return document; }
    public String getStateRegistration() { return stateRegistration; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public CustomerAddress getAddress() { return address; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
