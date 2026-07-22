package com.example.tech.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.*;

import com.example.tech.model.Category;
import com.example.tech.service.CategoryService;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

	private final CategoryService categoryService;

	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@GetMapping("/")
	public List<Category> getAllCategories() {
		return categoryService.getAllCategories();
	}

	@GetMapping("/{id}")
	public Optional<Category> getCategoryById(@PathVariable Long id) {
		return categoryService.getCategoryById(id);
	}

	@PostMapping("/")
	public Category createCategory(@RequestBody Category category) {
		return categoryService.createCategory(category);
	}

	@PutMapping("/{id}")
	public Category updateCategory(@PathVariable Long id,
	                               @RequestBody Category category) {
		return categoryService.updateCategory(id, category);
	}

	@DeleteMapping("/{id}")
	public void deleteCategory(@PathVariable Long id) {
		categoryService.deleteCategory(id);
	}
}