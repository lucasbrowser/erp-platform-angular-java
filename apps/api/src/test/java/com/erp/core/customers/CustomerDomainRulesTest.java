package com.erp.core.customers;

import com.erp.core.customers.domain.Customer;
import com.erp.core.customers.domain.CustomerAddress;
import com.erp.core.customers.domain.PersonType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerDomainRulesTest {
    private static final String VALID_CPF = "01234567890";
    private static final String VALID_CNPJ = "12ABC34501DE35";

    @Test
    void acceptsIndividualWithValidCpfAndPreservesLeadingZero() {
        Customer customer = customer(PersonType.INDIVIDUAL, "  Maria Silva  ", null,
                VALID_CPF, null, null, null, null);

        assertThat(customer.getName()).isEqualTo("Maria Silva");
        assertThat(customer.getDocument()).isEqualTo(VALID_CPF);
        assertThat(customer.getPersonType()).isEqualTo(PersonType.INDIVIDUAL);
    }

    @Test
    void rejectsIndividualWithoutCpfOrWithCnpj() {
        assertInvalid(PersonType.INDIVIDUAL, "Maria", null, null, null);
        assertInvalid(PersonType.INDIVIDUAL, "Maria", null, VALID_CNPJ, null);
    }

    @Test
    void rejectsCompanyOnlyFieldsForIndividual() {
        assertInvalid(PersonType.INDIVIDUAL, "Maria", "Fantasia", VALID_CPF, null);
        assertInvalid(PersonType.INDIVIDUAL, "Maria", null, VALID_CPF, "IE-123");
    }

    @Test
    void acceptsCompanyWithValidCnpjAndOptionalCompanyFields() {
        Customer customer = customer(PersonType.COMPANY, "Empresa Exemplo", "  Exemplo  ",
                VALID_CNPJ, "IE-123", null, null, null);

        assertThat(customer.getDocument()).isEqualTo(VALID_CNPJ);
        assertThat(customer.getTradeName()).isEqualTo("  Exemplo  ");
        assertThat(customer.getStateRegistration()).isEqualTo("IE-123");
    }

    @Test
    void normalizesMaskedLowercaseCnpjThroughTheExistingDocumentDomainRules() {
        Customer customer = customer(PersonType.COMPANY, "Empresa", null,
                "  12.abc.345/01de-35  ", null, null, null, null);

        assertThat(customer.getDocument()).isEqualTo(VALID_CNPJ);
    }

    @Test
    void rejectsCompanyWithoutCnpjOrWithCpf() {
        assertInvalid(PersonType.COMPANY, "Empresa", null, null, null);
        assertInvalid(PersonType.COMPANY, "Empresa", null, VALID_CPF, null);
    }

    @Test
    void doesNotExposeDocumentValueInValidationException() {
        String invalidCpf = "01234567891";

        assertThatThrownBy(() -> customer(PersonType.INDIVIDUAL, "Maria", null,
                invalidCpf, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining(invalidCpf);
    }

    @Test
    void acceptsCompanyWithoutOptionalFields() {
        Customer customer = customer(PersonType.COMPANY, "Empresa", null,
                VALID_CNPJ, null, null, null, null);

        assertThat(customer.getTradeName()).isNull();
        assertThat(customer.getStateRegistration()).isNull();
    }

    @Test
    void requiresTrimmedNonblankNameWithinPersistenceLimit() {
        Customer customer = customer(PersonType.INDIVIDUAL, "  Maria  ", null,
                VALID_CPF, null, null, null, null);
        assertThat(customer.getName()).isEqualTo("Maria");

        assertInvalid(PersonType.INDIVIDUAL, "   ", null, VALID_CPF, null);
        assertInvalid(PersonType.INDIVIDUAL, "N".repeat(161), null, VALID_CPF, null);
    }

    @Test
    void enforcesTradeNameAndStateRegistrationLimits() {
        assertInvalid(PersonType.COMPANY, "Empresa", "T".repeat(161), VALID_CNPJ, null);
        assertInvalid(PersonType.COMPANY, "Empresa", null, VALID_CNPJ, "I".repeat(33));
    }

    @Test
    void trimsAndValidatesOptionalEmail() {
        Customer customer = customer(PersonType.COMPANY, "Empresa", null,
                VALID_CNPJ, null, "  contato@example.com  ", null, null);
        assertThat(customer.getEmail()).isEqualTo("contato@example.com");

        assertInvalid(PersonType.COMPANY, "Empresa", null, VALID_CNPJ, null,
                "email-invalido", null, null);
        assertInvalid(PersonType.COMPANY, "Empresa", null, VALID_CNPJ, null,
                "e".repeat(243) + "@example.com", null, null);
    }

    @Test
    void allowsMissingEmailAndPhoneAndPreservesPhonePlus() {
        Customer customer = customer(PersonType.COMPANY, "Empresa", null,
                VALID_CNPJ, null, null, "+55 65 3333-4444", null);

        assertThat(customer.getEmail()).isNull();
        assertThat(customer.getPhone()).isEqualTo("+55 65 3333-4444");
    }

    @Test
    void enforcesPhoneLength() {
        assertInvalid(PersonType.COMPANY, "Empresa", null, VALID_CNPJ, null,
                null, "1".repeat(33), null);
    }

    @Test
    void permitsNoAddressAndPartiallyPopulatedAddress() {
        Customer withoutAddress = customer(PersonType.COMPANY, "Empresa", null,
                VALID_CNPJ, null, null, null, null);
        CustomerAddress partial = new CustomerAddress(null, "Rua Central", null, null,
                null, null, null, null);
        Customer withAddress = customer(PersonType.COMPANY, "Empresa", null,
                "12ABC34501DE35", null, null, null, partial);

        assertThat(withoutAddress.getAddress()).isNull();
        assertThat(withAddress.getAddress()).isSameAs(partial);
        assertThat(withAddress.getAddress().getStreet()).isEqualTo("Rua Central");
    }

    @Test
    void rejectsAddressFieldsExceedingPersistenceLimitsAndNormalizesCountryCode() {
        assertThat(new CustomerAddress(null, null, null, null, null, null, null, " br ")
                .getCountryCode()).isEqualTo("BR");
        assertThatThrownBy(() -> new CustomerAddress("P".repeat(17), null, null, null,
                null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CustomerAddress(null, null, null, null,
                null, null, null, "BRA")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullPersonType() {
        assertThatThrownBy(() -> customer(null, "Empresa", null, VALID_CNPJ,
                null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    private static void assertInvalid(PersonType type, String name, String tradeName,
                                      String document, String stateRegistration) {
        assertInvalid(type, name, tradeName, document, stateRegistration, null, null, null);
    }

    private static void assertInvalid(PersonType type, String name, String tradeName,
                                      String document, String stateRegistration,
                                      String email, String phone, CustomerAddress address) {
        assertThatThrownBy(() -> customer(type, name, tradeName, document, stateRegistration,
                email, phone, address)).isInstanceOf(IllegalArgumentException.class);
    }

    private static Customer customer(PersonType type, String name, String tradeName,
                                     String document, String stateRegistration,
                                     String email, String phone, CustomerAddress address) {
        return new Customer(type, name, tradeName, document, stateRegistration, email, phone, address);
    }
}
