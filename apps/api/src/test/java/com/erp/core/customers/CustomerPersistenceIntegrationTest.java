package com.erp.core.customers;

import com.erp.core.customers.domain.Customer;
import com.erp.core.customers.domain.CustomerAddress;
import com.erp.core.customers.domain.CustomerRepository;
import com.erp.core.customers.domain.PersonType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CustomerPersistenceIntegrationTest {
    @Autowired CustomerRepository customers;
    @Autowired JdbcTemplate jdbc;

    @Test
    void connectsToPostgresAndFlywayAppliedCustomerMigration() throws Exception {
        try (Connection connection = jdbc.getDataSource().getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }

        Integer successfulV5Migrations = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '5' AND success = true",
                Integer.class);
        assertThat(successfulV5Migrations).isEqualTo(1);

        String countryCodeType = jdbc.queryForObject(
                "SELECT data_type FROM information_schema.columns "
                        + "WHERE table_schema = 'erp' AND table_name = 'customers' AND column_name = 'country_code'",
                String.class);
        Integer countryCodeLength = jdbc.queryForObject(
                "SELECT character_maximum_length FROM information_schema.columns "
                        + "WHERE table_schema = 'erp' AND table_name = 'customers' AND column_name = 'country_code'",
                Integer.class);
        assertThat(countryCodeType).isEqualTo("character varying");
        assertThat(countryCodeLength).isEqualTo(2);
    }

    @Test
    void persistsIndividualAndCompanyWithCanonicalDocumentStrings() {
        Customer individual = customers.saveAndFlush(customer(
                PersonType.INDIVIDUAL, "Pessoa Física", "01234567890", null, null));
        Customer company = customers.saveAndFlush(customer(
                PersonType.COMPANY, "Empresa Exemplo", "12ABC34501DE35", "Exemplo", "IE-123"));

        assertThat(customers.findById(individual.getId()).orElseThrow().getPersonType())
                .isEqualTo(PersonType.INDIVIDUAL);
        assertThat(customers.findById(individual.getId()).orElseThrow().getDocument())
                .isEqualTo("01234567890");
        assertThat(customers.findById(company.getId()).orElseThrow().getPersonType())
                .isEqualTo(PersonType.COMPANY);
        assertThat(customers.findById(company.getId()).orElseThrow().getDocument())
                .isEqualTo("12ABC34501DE35");
        assertThat(customers.findById(company.getId()).orElseThrow().getTradeName())
                .isEqualTo("Exemplo");
    }

    @Test
    void persistsCompleteAddressAndNullableAddress() {
        Customer withAddress = customers.saveAndFlush(customer(
                PersonType.COMPANY,
                "Empresa com endereço",
                "12ABC34501DE35",
                null,
                null,
                new CustomerAddress("78000000", "Rua Central", "100", "Sala 2",
                        "Centro", "Cuiabá", "MT", "BR")));
        Customer withoutAddress = customers.saveAndFlush(customer(
                PersonType.INDIVIDUAL, "Pessoa sem endereço", "28001238938", null, null));

        Customer loadedWithAddress = customers.findById(withAddress.getId()).orElseThrow();
        assertThat(loadedWithAddress.getAddress()).isNotNull();
        assertThat(loadedWithAddress.getAddress().getPostalCode()).isEqualTo("78000000");
        assertThat(loadedWithAddress.getAddress().getStreet()).isEqualTo("Rua Central");
        assertThat(loadedWithAddress.getAddress().getNumber()).isEqualTo("100");
        assertThat(loadedWithAddress.getAddress().getComplement()).isEqualTo("Sala 2");
        assertThat(loadedWithAddress.getAddress().getNeighborhood()).isEqualTo("Centro");
        assertThat(loadedWithAddress.getAddress().getCity()).isEqualTo("Cuiabá");
        assertThat(loadedWithAddress.getAddress().getState()).isEqualTo("MT");
        assertThat(loadedWithAddress.getAddress().getCountryCode()).isEqualTo("BR");
        assertThat(customers.findById(withoutAddress.getId()).orElseThrow().getAddress()).isNull();
    }

    @Test
    void documentIsUniqueEvenWhenExistingCustomerIsInactive() {
        Customer original = customers.saveAndFlush(customer(
                PersonType.COMPANY, "Cliente inativo", "12ABC34501DE35", null, null));
        jdbc.update("UPDATE erp.customers SET active = false WHERE id = ?", original.getId());

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO erp.customers (id, person_type, name, document, active, created_at, updated_at) "
                        + "VALUES (?, 'COMPANY', 'Documento repetido', ?, true, ?, ?)",
                UUID.randomUUID(), "12ABC34501DE35", Timestamp.from(Instant.now()), Timestamp.from(Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsMissingRequiredColumns() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO erp.customers (id, name, document, active, created_at, updated_at) "
                        + "VALUES (?, 'Sem tipo', '12345678901', true, ?, ?)",
                UUID.randomUUID(), Timestamp.from(Instant.now()), Timestamp.from(Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void migrationMarksRequiredColumnsAsNotNull() {
        Set<String> requiredColumns = Set.copyOf(jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_schema = 'erp' AND table_name = 'customers' AND is_nullable = 'NO'",
                String.class));

        assertThat(requiredColumns).contains(
                "id", "person_type", "name", "document", "active", "created_at", "updated_at");
    }

    @Test
    void databaseRejectsUnknownPersonType() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO erp.customers (id, person_type, name, document, active, created_at, updated_at) "
                        + "VALUES (?, 'OTHER', 'Tipo inválido', '12345678901', true, ?, ?)",
                UUID.randomUUID(), Timestamp.from(Instant.now()), Timestamp.from(Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRequiresElevenNumericDigitsForIndividualDocument() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO erp.customers (id, person_type, name, document, active, created_at, updated_at) "
                        + "VALUES (?, 'INDIVIDUAL', 'CPF inválido', '1234567890A', true, ?, ?)",
                UUID.randomUUID(), Timestamp.from(Instant.now()), Timestamp.from(Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Customer customer(PersonType type, String name, String document,
                              String tradeName, String stateRegistration) {
        return customer(type, name, document, tradeName, stateRegistration, null);
    }

    private Customer customer(PersonType type, String name, String document,
                              String tradeName, String stateRegistration, CustomerAddress address) {
        return new Customer(type, name, tradeName, document, stateRegistration,
                null, null, address);
    }
}
