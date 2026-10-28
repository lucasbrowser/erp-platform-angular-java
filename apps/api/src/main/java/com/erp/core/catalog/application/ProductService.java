package com.erp.core.catalog.application;

import com.erp.core.catalog.api.ProductRequest;
import com.erp.core.catalog.domain.Category;
import com.erp.core.catalog.domain.CategoryRepository;
import com.erp.core.catalog.domain.Product;
import com.erp.core.catalog.domain.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static com.erp.core.catalog.application.CatalogExceptions.Conflict;
import static com.erp.core.catalog.application.CatalogExceptions.ResourceNotFound;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository products;
    private final CategoryRepository categories;

    public ProductService(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @Transactional
    public Product create(ProductRequest request) {
        String sku = normalizeSku(request.sku());
        if (products.existsBySku(sku)) throw new Conflict("Product SKU already exists");
        Category category = activeCategory(request.categoryId());
        return products.saveAndFlush(new Product(sku, request.name().trim(), request.description(), category, request.price()));
    }

    public Product get(UUID id) {
        return products.findById(id).orElseThrow(() -> new ResourceNotFound("Product"));
    }

    public Page<Product> list(String name, String sku, UUID categoryId, Boolean active, Pageable pageable) {
        Specification<Product> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if (name != null && !name.isBlank()) {
            String pattern = "%" + escapeLike(name.trim().toLowerCase(Locale.ROOT)) + "%";
            specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\'));
        }
        if (sku != null && !sku.isBlank()) {
            String pattern = "%" + escapeLike(normalizeSku(sku)) + "%";
            specification = specification.and((root, query, cb) -> cb.like(root.get("sku"), pattern, '\\'));
        }
        if (categoryId != null) specification = specification.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        if (active != null) specification = specification.and((root, query, cb) -> cb.equal(root.get("active"), active));
        return products.findAll(specification, pageable);
    }

    @Transactional
    public Product update(UUID id, ProductRequest request) {
        Product product = get(id);
        String sku = normalizeSku(request.sku());
        if (!product.getSku().equals(sku) && products.existsBySku(sku)) throw new Conflict("Product SKU already exists");
        Category category = categoryForUpdate(request.categoryId(), product);
        product.update(sku, request.name().trim(), request.description(), category, request.price());
        return products.saveAndFlush(product);
    }

    @Transactional
    public void deactivate(UUID id) {
        Product product = get(id);
        product.deactivate();
        products.saveAndFlush(product);
    }

    private Category activeCategory(UUID id) {
        Category category = categories.findById(id).orElseThrow(() -> new ResourceNotFound("Category"));
        if (!category.isActive()) throw new Conflict("Products can only be assigned to active categories");
        return category;
    }

    private Category categoryForUpdate(UUID requestedId, Product currentProduct) {
        Category category = categories.findById(requestedId).orElseThrow(() -> new ResourceNotFound("Category"));
        boolean unchangedAssociation = currentProduct.getCategory().getId().equals(requestedId);
        if (!category.isActive() && !unchangedAssociation) {
            throw new Conflict("Products can only be assigned to active categories");
        }
        return category;
    }

    private String normalizeSku(String sku) { return sku.trim().toUpperCase(Locale.ROOT); }

    private String escapeLike(String value) { return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"); }
}
