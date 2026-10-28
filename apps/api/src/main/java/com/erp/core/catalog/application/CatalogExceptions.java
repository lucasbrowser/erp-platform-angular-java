package com.erp.core.catalog.application;

public final class CatalogExceptions {
    private CatalogExceptions() { }

    public static class ResourceNotFound extends RuntimeException {
        public ResourceNotFound(String resource) { super(resource + " not found"); }
    }

    public static class Conflict extends RuntimeException {
        public Conflict(String message) { super(message); }
    }
}
