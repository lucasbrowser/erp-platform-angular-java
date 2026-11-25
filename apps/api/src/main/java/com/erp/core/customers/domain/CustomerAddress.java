package com.erp.core.customers.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Locale;
import java.util.regex.Pattern;

@Embeddable
public class CustomerAddress {
    private static final Pattern COUNTRY_CODE = Pattern.compile("[A-Za-z]{2}");

    @Column(name = "postal_code", length = 16)
    private String postalCode;

    @Column(name = "street", length = 160)
    private String street;

    @Column(name = "number", length = 32)
    private String number;

    @Column(name = "complement", length = 120)
    private String complement;

    @Column(name = "neighborhood", length = 120)
    private String neighborhood;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "state", length = 64)
    private String state;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    protected CustomerAddress() { }

    public CustomerAddress(String postalCode, String street, String number, String complement,
                           String neighborhood, String city, String state, String countryCode) {
        this.postalCode = optionalLength(postalCode, "CEP", 16);
        this.street = optionalLength(street, "Logradouro", 160);
        this.number = optionalLength(number, "Número do endereço", 32);
        this.complement = optionalLength(complement, "Complemento", 120);
        this.neighborhood = optionalLength(neighborhood, "Bairro", 120);
        this.city = optionalLength(city, "Cidade", 120);
        this.state = optionalLength(state, "Estado", 64);
        this.countryCode = normalizeCountryCode(countryCode);
    }

    private static String optionalLength(String value, String field, int maxLength) {
        if (value != null && value.codePointCount(0, value.length()) > maxLength) {
            throw new IllegalArgumentException(field + " excede o limite de " + maxLength + " caracteres.");
        }
        return value;
    }

    private static String normalizeCountryCode(String countryCode) {
        if (countryCode == null) {
            return null;
        }
        String normalized = countryCode.strip();
        if (!COUNTRY_CODE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Código do país deve usar duas letras ISO alpha-2.");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    public String getPostalCode() { return postalCode; }
    public String getStreet() { return street; }
    public String getNumber() { return number; }
    public String getComplement() { return complement; }
    public String getNeighborhood() { return neighborhood; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getCountryCode() { return countryCode; }
}
