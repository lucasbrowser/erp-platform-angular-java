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
import java.util.regex.Pattern;

@Entity
@Table(name = "customers", schema = "erp")
public class Customer {
    private static final Pattern BASIC_EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

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
        if (personType == null) {
            throw new IllegalArgumentException("Tipo de pessoa é obrigatório.");
        }
        this.id = UUID.randomUUID();
        this.personType = personType;
        this.name = requiredTrimmed(name, "Nome", 160);
        this.tradeName = optionalLength(tradeName, "Nome fantasia", 160);
        this.document = normalizeAndValidateDocument(personType, document);
        this.stateRegistration = optionalLength(stateRegistration, "Inscrição estadual", 32);
        this.email = normalizeEmail(email);
        this.phone = optionalLength(phone, "Telefone", 32);
        if (personType == PersonType.INDIVIDUAL && this.tradeName != null) {
            throw new IllegalArgumentException("Nome fantasia não é permitido para pessoa física.");
        }
        if (personType == PersonType.INDIVIDUAL && this.stateRegistration != null) {
            throw new IllegalArgumentException("Inscrição estadual não é permitida para pessoa física.");
        }
        this.address = address;
        this.active = true;
    }

    private static String requiredTrimmed(String value, String field, int maxLength) {
        if (value == null) {
            throw new IllegalArgumentException(field + " é obrigatório.");
        }
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " é obrigatório.");
        }
        return optionalLength(normalized, field, maxLength);
    }

    private static String optionalLength(String value, String field, int maxLength) {
        if (value != null && value.codePointCount(0, value.length()) > maxLength) {
            throw new IllegalArgumentException(field + " excede o limite de " + maxLength + " caracteres.");
        }
        return value;
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.strip();
        if (normalized.isEmpty()) {
            return null;
        }
        optionalLength(normalized, "E-mail", 254);
        if (!BASIC_EMAIL.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Formato de e-mail inválido.");
        }
        return normalized;
    }

    private static String normalizeAndValidateDocument(PersonType personType, String document) {
        if (personType == PersonType.INDIVIDUAL) {
            String cpf = TaxDocumentNormalizer.normalizeCpf(document);
            if (!TaxDocumentValidator.isValidCpf(cpf)) {
                throw new IllegalArgumentException("CPF inválido.");
            }
            return cpf;
        }

        String cnpj = TaxDocumentNormalizer.normalizeCnpj(document);
        if (!TaxDocumentValidator.isValidCnpj(cnpj)) {
            throw new IllegalArgumentException("CNPJ inválido.");
        }
        return cnpj;
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
