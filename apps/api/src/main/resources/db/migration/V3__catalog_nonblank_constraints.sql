ALTER TABLE erp.categories
    ADD CONSTRAINT ck_categories_name_trimmed_nonblank
    CHECK (name = btrim(name) AND length(name) > 0);

ALTER TABLE erp.products
    ADD CONSTRAINT ck_products_name_trimmed_nonblank
    CHECK (name = btrim(name) AND length(name) > 0),
    ADD CONSTRAINT ck_products_sku_nonblank
    CHECK (length(sku) > 0);
