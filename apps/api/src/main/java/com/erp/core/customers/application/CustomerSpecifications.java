package com.erp.core.customers.application;

import com.erp.core.customers.domain.Customer;
import com.erp.core.customers.domain.PersonType;
import com.erp.core.customers.domain.TaxDocumentNormalizer;
import com.erp.core.customers.domain.TaxDocumentValidator;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Builds database-side customer search predicates.
 *
 * <p>Callers must enforce the customer security policy before applying a document filter:
 * FINANCE must not be allowed to search by CPF/CNPJ.</p>
 */
final class CustomerSpecifications {
    private CustomerSpecifications() { }

    static Specification<Customer> withFilters(String name, String document,
                                               PersonType personType, Boolean active) {
        Specification<Customer> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.conjunction();

        if (name != null && !name.isBlank()) {
            String pattern = "%" + escapeLike(name.strip().toLowerCase(Locale.ROOT)) + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern, '\\'));
        }

        if (document != null) {
            String canonicalDocument = normalizeDocument(document);
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("document"), canonicalDocument));
        }

        if (personType != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("personType"), personType));
        }

        if (active != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("active"), active));
        }

        return specification;
    }

    private static String normalizeDocument(String document) {
        final String cpf;
        try {
            cpf = TaxDocumentNormalizer.normalizeCpf(document);
        } catch (IllegalArgumentException invalidCpfFormat) {
            return normalizeCnpj(document);
        }
        if (!TaxDocumentValidator.isValidCpf(cpf)) {
            throw new IllegalArgumentException("Documento inválido.");
        }
        return cpf;
    }

    private static String normalizeCnpj(String document) {
        final String cnpj;
        try {
            cnpj = TaxDocumentNormalizer.normalizeCnpj(document);
        } catch (IllegalArgumentException invalidCnpjFormat) {
            throw new IllegalArgumentException("Formato de documento inválido.");
        }
        if (!TaxDocumentValidator.isValidCnpj(cnpj)) {
            throw new IllegalArgumentException("Documento inválido.");
        }
        return cnpj;
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
