package com.example.tech.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.tech.model.Category;
import com.example.tech.repository.CategoryRepository;

@Service
public class CategoryService {

	private final CategoryRepository categoryRepository;
	
	public CategoryService(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}
	
	// Get all categories
	public List<Category> getAllCategories(){
		 return categoryRepository.findAll();
	}
	
	// Get Category by ID
	public Optional<Category> getCategoryById(Long id){
		return categoryRepository.findById(id);
	}
	
	// Add Category
	public Category createCategory(Category category) {
		return categoryRepository.save(category);
	}
	
	// Update Category by Id
	public Category updateCategory(Long id, Category updatedCategory) {
		return categoryRepository.findById(id).map( category -> {
			return categoryRepository.save(updatedCategory);
		}).orElseThrow(() -> new RuntimeException("Category not found with id " + id));
	}
	
	// Delete Category by Id
	public void deleteCategory(Long id) {
		categoryRepository.deleteById(id);
	}
	
}
