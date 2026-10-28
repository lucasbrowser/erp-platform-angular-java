package com.erp.core.catalog.application;

import com.erp.core.catalog.api.CategoryRequest;
import com.erp.core.catalog.domain.Category;
import com.erp.core.catalog.domain.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static com.erp.core.catalog.application.CatalogExceptions.Conflict;
import static com.erp.core.catalog.application.CatalogExceptions.ResourceNotFound;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categories;

    public CategoryService(CategoryRepository categories) { this.categories = categories; }

    @Transactional
    public Category create(CategoryRequest request) {
        String name = normalizeName(request.name());
        if (categories.existsByNameIgnoreCase(name)) throw new Conflict("Category name already exists");
        return categories.saveAndFlush(new Category(name, request.description()));
    }

    public Category get(UUID id) {
        return categories.findById(id).orElseThrow(() -> new ResourceNotFound("Category"));
    }

    public Page<Category> list(Pageable pageable) { return categories.findAll(pageable); }

    @Transactional
    public Category update(UUID id, CategoryRequest request) {
        Category category = get(id);
        String name = normalizeName(request.name());
        if (!category.getName().equalsIgnoreCase(name) && categories.existsByNameIgnoreCase(name)) {
            throw new Conflict("Category name already exists");
        }
        category.update(name, request.description());
        return categories.saveAndFlush(category);
    }

    @Transactional
    public void deactivate(UUID id) {
        Category category = get(id);
        category.deactivate();
        categories.saveAndFlush(category);
    }

    private String normalizeName(String name) { return name.trim(); }
}
