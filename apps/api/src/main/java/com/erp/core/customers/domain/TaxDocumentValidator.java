package com.erp.core.customers.domain;

/** Performs local CPF/CNPJ format and check-digit validation without external lookups. */
public final class TaxDocumentValidator {
    private TaxDocumentValidator() { }

    public static boolean isValidCpf(String document) {
        final String cpf;
        try {
            cpf = TaxDocumentNormalizer.normalizeCpf(document);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        if (hasRepeatedCharacters(cpf)) {
            return false;
        }

        int firstDigit = cpfCheckDigit(cpf, 9, 1);
        int secondDigit = cpfCheckDigit(cpf, 10, 0);
        return cpf.charAt(9) - '0' == firstDigit && cpf.charAt(10) - '0' == secondDigit;
    }

    public static boolean isValidCnpj(String document) {
        final String cnpj;
        try {
            cnpj = TaxDocumentNormalizer.normalizeCnpj(document);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        if (hasRepeatedCharacters(cnpj)) {
            return false;
        }

        int firstDigit = cnpjCheckDigit(cnpj, 12);
        int secondDigit = cnpjCheckDigit(cnpj, 13);
        return cnpj.charAt(12) - '0' == firstDigit && cnpj.charAt(13) - '0' == secondDigit;
    }

    private static int cpfCheckDigit(String cpf, int length, int firstWeight) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += (cpf.charAt(index) - '0') * (firstWeight + index);
        }
        int remainder = sum % 11;
        return remainder == 10 ? 0 : remainder;
    }

    private static int cnpjCheckDigit(String cnpj, int length) {
        int sum = 0;
        int weight = 2;
        for (int index = length - 1; index >= 0; index--) {
            sum += cnpjValue(cnpj.charAt(index)) * weight;
            weight = weight == 9 ? 2 : weight + 1;
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int cnpjValue(char character) {
        return character >= '0' && character <= '9'
                ? character - '0'
                : character - 48;
    }

    private static boolean hasRepeatedCharacters(String document) {
        char first = document.charAt(0);
        for (int index = 1; index < document.length(); index++) {
            if (document.charAt(index) != first) {
                return false;
            }
        }
        return true;
    }
}
