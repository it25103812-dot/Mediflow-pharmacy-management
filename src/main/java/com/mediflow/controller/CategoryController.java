package com.mediflow.controller;

import com.mediflow.dto.CategoryRequest;
import com.mediflow.entity.Category;
import jakarta.validation.Valid;
import com.mediflow.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Object list(@RequestParam(defaultValue = "") String search,
                       @RequestParam(required = false) Boolean active,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(defaultValue = "false") boolean all) {
        if (all) {
            return categoryService.findAllActive();
        }
        return categoryService.search(search, active, page, size);
    }

    @PostMapping
    public Category create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{id}")
    public Category update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        boolean deleted = categoryService.delete(id);
        return Map.of("deleted", deleted);
    }
}
