package com.erp.core.customers.application;

import com.erp.core.customers.domain.Customer;
import com.erp.core.customers.domain.CustomerRepository;
import com.erp.core.customers.domain.PersonType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CustomerSearchIntegrationTest {
    private static final PageRequest PAGE = PageRequest.of(0, 100);

    @Autowired CustomerRepository customers;
    @Autowired JdbcTemplate jdbc;

    @Test
    void searchesByPartialName() {
        Customer expected = customer(PersonType.COMPANY, "Comercial Horizonte Ltda.",
                "18.781.203/0001-28");
        customer(PersonType.COMPANY, "Oficina Central", "12.ABC.345/01DE-35");

        assertThat(ids(find("Horizonte", null, null, null))).containsExactly(expected.getId());
    }

    @Test
    void searchesNameWithoutCaseSensitivity() {
        Customer expected = customer(PersonType.INDIVIDUAL, "Maria Oliveira", "28001238938");

        assertThat(ids(find("mArIa", null, null, null))).containsExactly(expected.getId());
    }

    @Test
    void trimsExternalSpacesFromNameFilter() {
        Customer expected = customer(PersonType.COMPANY, "Alpha Comércio", "12.ABC.345/01DE-35");

        assertThat(ids(find("  Comércio  ", null, null, null))).containsExactly(expected.getId());
    }

    @Test
    void treatsLikeWildcardsAndEscapeCharacterLiterally() {
        Customer percent = customer(PersonType.COMPANY, "Cliente 50% Real", "18.781.203/0001-28");
        Customer underscore = customer(PersonType.INDIVIDUAL, "Conta_Azul", "01234567890");
        Customer backslash = customer(PersonType.COMPANY, "Cliente\\Especial", "12.ABC.345/01DE-35");
        customer(PersonType.INDIVIDUAL, "Cliente 50X Real", "28001238938");

        assertThat(ids(find("%", null, null, null))).containsExactly(percent.getId());
        assertThat(ids(find("_", null, null, null))).containsExactly(underscore.getId());
        assertThat(ids(find("\\", null, null, null))).containsExactly(backslash.getId());
    }

    @Test
    void searchesCpfExactlyAndPreservesLeadingZeroes() {
        Customer expected = customer(PersonType.INDIVIDUAL, "Pessoa com zero inicial", "01234567890");
        customer(PersonType.INDIVIDUAL, "Outra pessoa", "28001238938");

        assertThat(ids(find(null, "01234567890", null, null))).containsExactly(expected.getId());
    }

    @Test
    void searchesNumericCnpjExactly() {
        Customer expected = customer(PersonType.COMPANY, "Empresa numérica", "18.781.203/0001-28");
        customer(PersonType.COMPANY, "Empresa alfanumérica", "12.ABC.345/01DE-35");

        assertThat(ids(find(null, "18781203000128", null, null))).containsExactly(expected.getId());
    }

    @Test
    void searchesAlphanumericCnpjAndKeepsItsLetters() {
        Customer expected = customer(PersonType.COMPANY, "Empresa alfanumérica", "12.ABC.345/01DE-35");

        assertThat(ids(find(null, "12ABC34501DE35", null, null))).containsExactly(expected.getId());
    }

    @Test
    void canonicalizesMaskedDocumentBeforeExactSearch() {
        Customer cpf = customer(PersonType.INDIVIDUAL, "Pessoa", "28001238938");
        Customer cnpj = customer(PersonType.COMPANY, "Empresa", "12ABC34501DE35");

        assertThat(ids(find(null, "280.012.389-38", null, null))).containsExactly(cpf.getId());
        assertThat(ids(find(null, "12.abc.345/01de-35", null, null))).containsExactly(cnpj.getId());
    }

    @Test
    void rejectsInvalidCpfWithGenericMessageInsteadOfTreatingItAsCnpj() {
        String invalidCpf = "01234567891";

        assertThatThrownBy(() -> find(null, invalidCpf, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Documento inválido.")
                .hasMessageNotContaining(invalidCpf);
    }

    @Test
    void filtersOnlyIndividualCustomers() {
        Customer individual = customer(PersonType.INDIVIDUAL, "Pessoa", "28001238938");
        customer(PersonType.COMPANY, "Empresa", "12ABC34501DE35");

        assertThat(ids(find(null, null, PersonType.INDIVIDUAL, null)))
                .containsExactly(individual.getId());
    }

    @Test
    void filtersOnlyCompanyCustomers() {
        customer(PersonType.INDIVIDUAL, "Pessoa", "28001238938");
        Customer company = customer(PersonType.COMPANY, "Empresa", "12ABC34501DE35");

        assertThat(ids(find(null, null, PersonType.COMPANY, null))).containsExactly(company.getId());
    }

    @Test
    void filtersOnlyActiveCustomers() {
        Customer active = customer(PersonType.COMPANY, "Ativo", "12ABC34501DE35");
        Customer inactive = customer(PersonType.INDIVIDUAL, "Inativo", "28001238938");
        setActive(inactive, false);

        assertThat(ids(find(null, null, null, true))).containsExactly(active.getId());
    }

    @Test
    void filtersOnlyInactiveCustomersWhenActiveIsFalse() {
        Customer active = customer(PersonType.COMPANY, "Ativo", "12ABC34501DE35");
        Customer inactive = customer(PersonType.INDIVIDUAL, "Inativo", "28001238938");
        setActive(inactive, false);

        assertThat(ids(find(null, null, null, false))).containsExactly(inactive.getId());
        assertThat(active.getId()).isNotEqualTo(inactive.getId());
    }

    @Test
    void includesActiveAndInactiveCustomersWhenStatusFilterIsAbsent() {
        Customer active = customer(PersonType.COMPANY, "Ativo", "12ABC34501DE35");
        Customer inactive = customer(PersonType.INDIVIDUAL, "Inativo", "28001238938");
        setActive(inactive, false);

        assertThat(ids(find(null, null, null, null))).containsExactlyInAnyOrder(active.getId(), inactive.getId());
    }

    @Test
    void combinesNameAndPersonTypeWithAnd() {
        Customer expected = customer(PersonType.INDIVIDUAL, "Norte Cliente", "01234567890");
        customer(PersonType.COMPANY, "Norte Empresa", "12ABC34501DE35");
        customer(PersonType.INDIVIDUAL, "Sul Cliente", "28001238938");

        assertThat(ids(find("Norte", null, PersonType.INDIVIDUAL, null)))
                .containsExactly(expected.getId());
    }

    @Test
    void combinesNameAndActiveStatusWithAnd() {
        Customer expected = customer(PersonType.COMPANY, "Cliente ativo", "12ABC34501DE35");
        Customer inactive = customer(PersonType.INDIVIDUAL, "Cliente inativo", "01234567890");
        customer(PersonType.COMPANY, "Fornecedor ativo", "18.781.203/0001-28");
        setActive(inactive, false);

        assertThat(ids(find("Cliente", null, null, true))).containsExactly(expected.getId());
    }

    @Test
    void combinesExactDocumentAndActiveStatusWithAnd() {
        Customer expected = customer(PersonType.COMPANY, "Cliente inativo", "12ABC34501DE35");
        customer(PersonType.INDIVIDUAL, "Cliente ativo", "01234567890");
        setActive(expected, false);

        assertThat(ids(find(null, "12.abc.345/01de-35", null, false)))
                .containsExactly(expected.getId());
    }

    @Test
    void combinesPersonTypeAndActiveStatusWithAnd() {
        Customer expected = customer(PersonType.INDIVIDUAL, "PF inativo", "01234567890");
        Customer activeIndividual = customer(PersonType.INDIVIDUAL, "PF ativo", "28001238938");
        Customer inactiveCompany = customer(PersonType.COMPANY, "PJ inativo", "12ABC34501DE35");
        setActive(expected, false);
        setActive(inactiveCompany, false);

        assertThat(ids(find(null, null, PersonType.INDIVIDUAL, false)))
                .containsExactly(expected.getId());
        assertThat(activeIndividual.getId()).isNotEqualTo(expected.getId());
    }

    @Test
    void combinesNamePersonTypeAndStatusWithAnd() {
        Customer expected = customer(PersonType.COMPANY, "Cliente Aurora", "12ABC34501DE35");
        Customer wrongType = customer(PersonType.INDIVIDUAL, "Cliente Boreal", "01234567890");
        Customer wrongStatus = customer(PersonType.COMPANY, "Cliente Boreal ativo", "18.781.203/0001-28");
        setActive(expected, false);

        assertThat(ids(find("Cliente", null, PersonType.COMPANY, false)))
                .containsExactly(expected.getId());
        assertThat(wrongType.getId()).isNotEqualTo(expected.getId());
        assertThat(wrongStatus.getId()).isNotEqualTo(expected.getId());
    }

    @Test
    void combinesAllFiltersAndCanonicalizesAlphanumericDocument() {
        Customer expected = customer(PersonType.COMPANY, "Cliente Aurora", "12ABC34501DE35");
        Customer wrongName = customer(PersonType.COMPANY, "Fornecedor Aurora", "18.781.203/0001-28");
        Customer wrongType = customer(PersonType.INDIVIDUAL, "Cliente Aurora PF", "01234567890");
        setActive(expected, false);

        assertThat(ids(find("  CLIENTE  ", "12.abc.345/01de-35", PersonType.COMPANY, false)))
                .containsExactly(expected.getId());
        assertThat(wrongName.getId()).isNotEqualTo(expected.getId());
        assertThat(wrongType.getId()).isNotEqualTo(expected.getId());
    }

    @Test
    void returnsCorrectZeroBasedPageMetadataWithDeterministicOrdering() {
        customer(PersonType.COMPANY, "Alpha", "18.781.203/0001-28");
        customer(PersonType.COMPANY, "Beta", "12ABC34501DE35");
        customer(PersonType.INDIVIDUAL, "Gamma", "01234567890");
        Pageable firstPageRequest = PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "name"));
        Pageable secondPageRequest = PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "name"));

        Page<Customer> firstPage = find(null, null, null, null, firstPageRequest);
        Page<Customer> secondPage = find(null, null, null, null, secondPageRequest);

        assertThat(firstPage.getNumber()).isZero();
        assertThat(firstPage.getSize()).isEqualTo(2);
        assertThat(firstPage.getContent()).extracting(Customer::getName)
                .containsExactly("Alpha", "Beta");
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.isFirst()).isTrue();
        assertThat(firstPage.isLast()).isFalse();

        assertThat(secondPage.getNumber()).isEqualTo(1);
        assertThat(secondPage.getContent()).extracting(Customer::getName).containsExactly("Gamma");
        assertThat(secondPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.getTotalPages()).isEqualTo(2);
        assertThat(secondPage.isLast()).isTrue();
    }

    @Test
    void traversesMultipleDatabasePagesWithoutRepeatingCustomers() {
        customer(PersonType.COMPANY, "Alpha", "18.781.203/0001-28");
        customer(PersonType.COMPANY, "Beta", "12ABC34501DE35");
        customer(PersonType.INDIVIDUAL, "Gamma", "01234567890");
        Sort stableOrder = Sort.by(Sort.Direction.ASC, "name");

        Page<Customer> first = find(null, null, null, null, PageRequest.of(0, 1, stableOrder));
        Page<Customer> second = find(null, null, null, null, PageRequest.of(1, 1, stableOrder));
        Page<Customer> last = find(null, null, null, null, PageRequest.of(2, 1, stableOrder));
        List<UUID> allIds = List.of(first.getContent().getFirst().getId(),
                second.getContent().getFirst().getId(), last.getContent().getFirst().getId());

        assertThat(first.getNumber()).isZero();
        assertThat(first.getSize()).isEqualTo(1);
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(3);
        assertThat(first.isFirst()).isTrue();
        assertThat(last.getNumber()).isEqualTo(2);
        assertThat(last.isLast()).isTrue();
        assertThat(allIds).doesNotHaveDuplicates();
        assertThat(Set.copyOf(allIds)).hasSize(3);
    }

    @Test
    void returnsEmptyPageWhenNoCustomersMatch() {
        customer(PersonType.COMPANY, "Cliente presente", "12ABC34501DE35");

        Page<Customer> page = find("ausente", null, null, null,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "name")));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getNumber()).isZero();
        assertThat(page.getSize()).isEqualTo(2);
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getTotalPages()).isZero();
    }

    @Test
    void sortsByEveryAllowlistedDatabaseField() {
        Customer zulu = customer(PersonType.INDIVIDUAL, "Zulu", "28001238938");
        Customer alpha = customer(PersonType.COMPANY, "Alpha", "12ABC34501DE35");
        Customer beta = customer(PersonType.COMPANY, "Beta", "18781203000128");
        setActive(beta, false);
        setTimestamps(zulu, "2024-01-03T00:00:00Z", "2024-02-03T00:00:00Z");
        setTimestamps(alpha, "2024-01-01T00:00:00Z", "2024-02-01T00:00:00Z");
        setTimestamps(beta, "2024-01-02T00:00:00Z", "2024-02-02T00:00:00Z");

        assertSorted(List.of("name,asc"), alpha, beta, zulu);
        assertSorted(List.of("document,asc"), alpha, beta, zulu);
        assertSorted(List.of("personType,asc", "name,asc"), alpha, beta, zulu);
        assertSorted(List.of("personType,desc", "name,asc"), zulu, alpha, beta);
        assertSorted(List.of("active,asc", "name,asc"), beta, alpha, zulu);
        assertSorted(List.of("active,desc", "name,asc"), alpha, zulu, beta);
        assertSorted(List.of("createdAt,asc"), alpha, beta, zulu);
        assertSorted(List.of("createdAt,desc"), zulu, beta, alpha);
        assertSorted(List.of("updatedAt,asc"), alpha, beta, zulu);
        assertSorted(List.of("updatedAt,desc"), zulu, beta, alpha);
    }

    @Test
    void sortsInBothDirectionsAndAppliesDefaultNameAscendingInPostgres() {
        Customer zulu = customer(PersonType.COMPANY, "Zulu", "12ABC34501DE35");
        Customer alpha = customer(PersonType.COMPANY, "Alpha", "18781203000128");
        Customer beta = customer(PersonType.INDIVIDUAL, "Beta", "28001238938");
        setTimestamps(zulu, "2024-01-03T00:00:00Z", "2024-01-03T00:00:00Z");
        setTimestamps(alpha, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
        setTimestamps(beta, "2024-01-02T00:00:00Z", "2024-01-02T00:00:00Z");

        assertSorted(List.of("name,asc"), alpha, beta, zulu);
        assertSorted(List.of("name,desc"), zulu, beta, alpha);
        assertSorted(List.of("document,desc"), beta, alpha, zulu);
        assertSorted(List.of("createdAt,desc"), zulu, beta, alpha);
        assertThat(orderedIds(findSorted(null, null, null, null, null)))
                .containsExactly(alpha.getId(), beta.getId(), zulu.getId());
    }

    @Test
    void preservesTwoAndThreeCriteriaPrecedenceAndTieBreakers() {
        Customer companyB = customer(PersonType.COMPANY, "Same", "12ABC34501DE35");
        Customer companyA = customer(PersonType.COMPANY, "Same", "18781203000128");
        Customer individual = customer(PersonType.INDIVIDUAL, "Same", "28001238938");
        setActive(companyA, false);
        setActive(companyB, false);
        setTimestamps(companyA, "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
        setTimestamps(companyB, "2024-01-03T00:00:00Z", "2024-01-03T00:00:00Z");
        setTimestamps(individual, "2024-01-02T00:00:00Z", "2024-01-02T00:00:00Z");

        assertSorted(List.of("name,asc", "document,desc"), individual, companyA, companyB);
        assertSorted(List.of("name,asc", "personType,asc", "createdAt,desc"),
                companyB, companyA, individual);
        assertThat(CustomerSortPolicy.fromParameters(List.of("name,asc", "document,desc"))
                .stream().map(Sort.Order::getProperty)).containsExactly("name", "document");
    }

    @Test
    void combinesOrderingWithEachFilterAndCombinedFilters() {
        Customer alphaCompanyActive = customer(PersonType.COMPANY, "Alpha Cliente", "12ABC34501DE35");
        Customer betaCompanyInactive = customer(PersonType.COMPANY, "Beta Cliente", "18781203000128");
        Customer alphaIndividualActive = customer(PersonType.INDIVIDUAL, "Alpha Cliente PF", "01234567890");
        Customer alphaCompanyWrongName = customer(PersonType.COMPANY, "Alpha Fornecedor", "12345678000195");
        setActive(betaCompanyInactive, false);

        assertThat(orderedIds(findSorted("Alpha Cliente", null, null, null, List.of("name,desc"))))
                .containsExactly(alphaIndividualActive.getId(), alphaCompanyActive.getId());
        assertThat(orderedIds(findSorted(null, null, PersonType.COMPANY, null, List.of("name,asc"))))
                .containsExactly(alphaCompanyActive.getId(), alphaCompanyWrongName.getId(),
                        betaCompanyInactive.getId());
        assertThat(orderedIds(findSorted(null, null, null, false, List.of("name,asc"))))
                .containsExactly(betaCompanyInactive.getId());
        assertThat(orderedIds(findSorted("Alpha", null, PersonType.COMPANY, true,
                List.of("name,asc")))).containsExactly(alphaCompanyActive.getId(), alphaCompanyWrongName.getId());
    }

    @Test
    void keepsSortedCriteriaConsistentAcrossDatabasePagesWithoutDuplicates() {
        Customer alphaA = customer(PersonType.COMPANY, "Alpha", "12ABC34501DE35");
        Customer alphaB = customer(PersonType.COMPANY, "Alpha", "18781203000128");
        Customer beta = customer(PersonType.INDIVIDUAL, "Beta", "01234567890");
        Customer gamma = customer(PersonType.INDIVIDUAL, "Gamma", "28001238938");
        List<String> sort = List.of("name,asc", "document,desc");

        Page<Customer> first = findSortedPage(0, 2, null, null, null, null, sort);
        Page<Customer> second = findSortedPage(1, 2, null, null, null, null, sort);
        List<UUID> pageIds = java.util.stream.Stream.concat(first.getContent().stream(),
                        second.getContent().stream())
                .map(Customer::getId).toList();

        assertThat(first.getContent()).extracting(Customer::getId)
                .containsExactly(alphaB.getId(), alphaA.getId());
        assertThat(second.getContent()).extracting(Customer::getId)
                .containsExactly(beta.getId(), gamma.getId());
        assertThat(pageIds).doesNotHaveDuplicates().hasSize(4);
        assertThat(first.getTotalElements()).isEqualTo(4);
        assertThat(second.getTotalElements()).isEqualTo(4);
    }

    private void assertSorted(List<String> sort, Customer... expected) {
        List<UUID> actualIds = findSorted(null, null, null, null, sort).getContent().stream()
                .map(Customer::getId)
                .toList();
        List<UUID> expectedIds = java.util.Arrays.stream(expected)
                .map(Customer::getId)
                .toList();

        assertThat(actualIds).containsExactlyElementsOf(expectedIds);
    }

    private Page<Customer> findSorted(String name, String document, PersonType personType,
                                      Boolean active, List<String> sort) {
        return findSortedPage(0, 100, name, document, personType, active, sort);
    }

    private Page<Customer> findSortedPage(int page, int size, String name, String document,
                                          PersonType personType, Boolean active, List<String> sort) {
        Pageable pageable = PageRequest.of(page, size, CustomerSortPolicy.fromParameters(sort));
        return find(name, document, personType, active, pageable);
    }

    private void setTimestamps(Customer customer, String createdAt, String updatedAt) {
        jdbc.update("UPDATE erp.customers SET created_at = ?, updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse(createdAt)), Timestamp.from(Instant.parse(updatedAt)),
                customer.getId());
    }

    private Customer customer(PersonType type, String name, String document) {
        return customers.saveAndFlush(new Customer(type, name, null, document,
                null, null, null, null));
    }

    private void setActive(Customer customer, boolean active) {
        jdbc.update("UPDATE erp.customers SET active = ? WHERE id = ?", active, customer.getId());
    }

    private Page<Customer> find(String name, String document, PersonType personType, Boolean active) {
        return find(name, document, personType, active, PAGE);
    }

    private Page<Customer> find(String name, String document, PersonType personType,
                                Boolean active, Pageable pageable) {
        return customers.findAll(
                CustomerSpecifications.withFilters(name, document, personType, active), pageable);
    }

    private Set<UUID> ids(Page<Customer> page) {
        return page.getContent().stream().map(Customer::getId).collect(Collectors.toSet());
    }

    private List<UUID> orderedIds(Page<Customer> page) {
        return page.getContent().stream().map(Customer::getId).toList();
    }
}
