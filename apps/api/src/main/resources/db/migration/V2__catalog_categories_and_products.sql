CREATE TABLE erp.categories (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uq_categories_name_ci ON erp.categories (lower(name));

CREATE TABLE erp.products (
    id UUID PRIMARY KEY,
    sku VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(2000),
    category_id UUID NOT NULL REFERENCES erp.categories (id),
    price NUMERIC(14, 2) NOT NULL CHECK (price >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_products_sku_normalized CHECK (sku = upper(btrim(sku)))
);

CREATE UNIQUE INDEX uq_products_sku ON erp.products (sku);
CREATE INDEX ix_products_category_id ON erp.products (category_id);
CREATE INDEX ix_products_active_name ON erp.products (active, lower(name));
