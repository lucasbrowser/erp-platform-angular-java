ALTER TABLE erp.customers
    ALTER COLUMN country_code TYPE VARCHAR(2)
    USING rtrim(country_code)::VARCHAR(2);
