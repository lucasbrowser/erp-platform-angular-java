package com.erp.core.customers;

import com.erp.core.customers.domain.TaxDocumentValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaxDocumentValidatorTest {
    @Test
    void validatesOfficialCpfExampleWithAndWithoutMask() {
        assertThat(TaxDocumentValidator.isValidCpf("28001238938")).isTrue();
        assertThat(TaxDocumentValidator.isValidCpf("280.012.389-38")).isTrue();
    }

    @Test
    void validatesCpfWithLeadingZeroesAndRejectsInvalidChecksumsAndRepeatedDigits() {
        // For base 012345678, the RFB manual's weighted sums are 240 (DV 9) and 285 (DV 0).
        assertThat(TaxDocumentValidator.isValidCpf("01234567890")).isTrue();
        assertThat(TaxDocumentValidator.isValidCpf("28001238939")).isFalse();
        assertThat(TaxDocumentValidator.isValidCpf("11111111111")).isFalse();
        assertThat(TaxDocumentValidator.isValidCpf("1234567890")).isFalse();
        assertThat(TaxDocumentValidator.isValidCpf("2800123893A")).isFalse();
    }

    @Test
    void validatesOfficialNumericLegacyCnpjExampleWithAndWithoutMask() {
        assertThat(TaxDocumentValidator.isValidCnpj("18781203000128")).isTrue();
        assertThat(TaxDocumentValidator.isValidCnpj("18.781.203/0001-28")).isTrue();
    }

    @Test
    void rejectsInvalidNumericCnpjChecksumsAndRepeatedDigits() {
        assertThat(TaxDocumentValidator.isValidCnpj("18781203000127")).isFalse();
        assertThat(TaxDocumentValidator.isValidCnpj("11111111111111")).isFalse();
        assertThat(TaxDocumentValidator.isValidCnpj("1878120300012A")).isFalse();
    }

    @Test
    void validatesOfficialAlphanumericCnpjExampleAndNormalizesLowercase() {
        assertThat(TaxDocumentValidator.isValidCnpj("12ABC34501DE35")).isTrue();
        assertThat(TaxDocumentValidator.isValidCnpj("12.abc.345/01de-35")).isTrue();
    }

    @Test
    void rejectsInvalidAlphanumericCnpjChecksumsAndMalformedDocuments() {
        assertThat(TaxDocumentValidator.isValidCnpj("12ABC34501DE36")).isFalse();
        assertThat(TaxDocumentValidator.isValidCnpj("12.ABC.345/01D_-35")).isFalse();
        assertThat(TaxDocumentValidator.isValidCnpj(null)).isFalse();
    }
}
