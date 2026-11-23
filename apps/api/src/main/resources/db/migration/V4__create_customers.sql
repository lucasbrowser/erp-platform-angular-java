CREATE TABLE erp.customers (
    id UUID PRIMARY KEY,
    person_type VARCHAR(16) NOT NULL,
    name VARCHAR(160) NOT NULL,
    trade_name VARCHAR(160),
    document VARCHAR(14) NOT NULL,
    state_registration VARCHAR(32),
    email VARCHAR(254),
    phone VARCHAR(32),
    postal_code VARCHAR(16),
    street VARCHAR(160),
    number VARCHAR(32),
    complement VARCHAR(120),
    neighborhood VARCHAR(120),
    city VARCHAR(120),
    state VARCHAR(64),
    country_code CHAR(2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT ck_customers_person_type
        CHECK (person_type IN ('INDIVIDUAL', 'COMPANY')),
    CONSTRAINT ck_customers_name_nonblank_trimmed
        CHECK (length(name) > 0 AND name = btrim(name)),
    CONSTRAINT ck_customers_person_fields
        CHECK (
            person_type = 'COMPANY'
            OR (trade_name IS NULL AND state_registration IS NULL)
        ),
    CONSTRAINT ck_customers_document_format_by_person_type
        CHECK (
            (person_type = 'INDIVIDUAL' AND document ~ '^[0-9]{11}$')
            OR (person_type = 'COMPANY' AND length(document) = 14)
        )
);

CREATE UNIQUE INDEX uq_customers_document
    ON erp.customers (document);

CREATE INDEX ix_customers_active_name
    ON erp.customers (active, lower(name));
