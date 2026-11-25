package com.erp.core.customers.domain;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Normalizes the supported CPF and CNPJ presentations to their canonical string form. */
public final class TaxDocumentNormalizer {
    private static final Pattern CPF_UNMASKED = Pattern.compile("[0-9]{11}");
    private static final Pattern CPF_MASKED = Pattern.compile("([0-9]{3})\\.([0-9]{3})\\.([0-9]{3})-([0-9]{2})");
    private static final Pattern CNPJ_UNMASKED = Pattern.compile("[A-Za-z0-9]{12}[0-9]{2}");
    private static final Pattern CNPJ_MASKED = Pattern.compile(
            "([A-Za-z0-9]{2})\\.([A-Za-z0-9]{3})\\.([A-Za-z0-9]{3})/([A-Za-z0-9]{4})-([0-9]{2})");

    private TaxDocumentNormalizer() { }

    public static String normalizeCpf(String document) {
        String value = requireInput(document, "CPF");
        if (CPF_UNMASKED.matcher(value).matches()) {
            return value;
        }

        Matcher matcher = CPF_MASKED.matcher(value);
        if (matcher.matches()) {
            return matcher.group(1) + matcher.group(2) + matcher.group(3) + matcher.group(4);
        }
        throw invalidFormat("CPF");
    }

    public static String normalizeCnpj(String document) {
        String value = requireInput(document, "CNPJ");
        String canonical;
        if (CNPJ_UNMASKED.matcher(value).matches()) {
            canonical = value;
        } else {
            Matcher matcher = CNPJ_MASKED.matcher(value);
            if (!matcher.matches()) {
                throw invalidFormat("CNPJ");
            }
            canonical = matcher.group(1) + matcher.group(2) + matcher.group(3)
                    + matcher.group(4) + matcher.group(5);
        }
        return canonical.toUpperCase(Locale.ROOT);
    }

    private static String requireInput(String document, String kind) {
        if (document == null || document.isBlank()) {
            throw invalidFormat(kind);
        }
        return document.strip();
    }

    private static IllegalArgumentException invalidFormat(String kind) {
        return new IllegalArgumentException("Formato de " + kind + " inválido.");
    }
}
