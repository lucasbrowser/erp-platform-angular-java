package com.erp.core.customers;

import com.erp.core.customers.domain.TaxDocumentNormalizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaxDocumentNormalizerTest {
    @Test
    void normalizesCpfWithoutMaskAndPreservesLeadingZeroes() {
        assertThat(TaxDocumentNormalizer.normalizeCpf(" 01234567890 ")).isEqualTo("01234567890");
    }

    @Test
    void removesOnlyTheOfficialCpfMask() {
        assertThat(TaxDocumentNormalizer.normalizeCpf("280.012.389-38")).isEqualTo("28001238938");
    }

    @Test
    void rejectsNullEmptyMalformedAndUnexpectedCpfCharacters() {
        assertThatThrownBy(() -> TaxDocumentNormalizer.normalizeCpf(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TaxDocumentNormalizer.normalizeCpf("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TaxDocumentNormalizer.normalizeCpf("2800123893"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TaxDocumentNormalizer.normalizeCpf("280.012389-38"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TaxDocumentNormalizer.normalizeCpf("280.012.389/38"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TaxDocumentNormalizer.normalizeCpf("2800123893A"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalizesNumericCnpjAndAcceptsOnlyItsOfficialMask() {
        assertThat(TaxDocumentNormalizer.normalizeCnpj("18.781.203/0001-28"))
                .isEqualTo("18781203000128");
        assertThat(TaxDocumentNormalizer.normalizeCnpj("18781203000128"))
                .isEqualTo("18781203000128");
    }

    @Test
    void normalizesAlphanumericCnpjToUppercaseAndKeepsCharacterPositions() {
        assertThat(TaxDocumentNormalizer.normalizeCnpj(" 12.abc.345/01de-35 "))
                .isEqualTo("12ABC34501DE35");
        assertThat(TaxDocumentNormalizer.normalizeCnpj("12ABC34501DE35"))
                .isEqualTo("12ABC34501DE35");
    }

    @Test
    void rejectsNullEmptyMalformedAndUnexpectedCnpjCharacters() {
        assertThatThrownBy(() -> TaxDocumentNormalizer.normalizeCnpj(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TaxDocumentNormalizer.normalizeCnpj(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TaxDocumentNormalizer.normalizeCnpj("12ABC34501DE3"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TaxDocumentNormalizer.normalizeCnpj("12.ABC345/01DE-35"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TaxDocumentNormalizer.normalizeCnpj("12.ABC.345/01DE-3A"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TaxDocumentNormalizer.normalizeCnpj("12.ABC.345/01D_-35"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
