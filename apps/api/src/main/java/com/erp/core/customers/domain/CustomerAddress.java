package com.erp.core.customers.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CustomerAddress {
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
        this.postalCode = postalCode;
        this.street = street;
        this.number = number;
        this.complement = complement;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
        this.countryCode = countryCode;
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
