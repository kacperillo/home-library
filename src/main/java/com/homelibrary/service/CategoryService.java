package com.homelibrary.service;

import com.homelibrary.api.response.CategoryResponse;
import com.homelibrary.exception.HomeLibraryException;
import com.homelibrary.model.Category;
import com.homelibrary.repository.CategoryRepository;

import com.homelibrary.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

  private final CategoryRepository categoryRepository;
  private final SubcategoryRepository subcategoryRepository;

  public CategoryResponse addCategory(String categoryName) {
    Category category = new Category(categoryName);
    category = categoryRepository.save(category);
    return new CategoryResponse(category);
  }

  public List<CategoryResponse> getAllCategories() {
    List<Category> categories = categoryRepository.findAll();
    List<CategoryResponse> categoryResponseList = new ArrayList<>();
    categories.forEach(category -> categoryResponseList.add(new CategoryResponse(category)));
    return categoryResponseList;
  }

  public CategoryResponse updateCategoryName(Integer categoryId, String newCategoryName) {
    Category category = findCategory(categoryId);
    if (category.getName() == newCategoryName) {
      throw new HomeLibraryException(HttpStatus.CONFLICT, "Category name is the same");
    }
    category.setName(newCategoryName);
    category = categoryRepository.save(category);
    return new CategoryResponse(category);
  }

  public void deleteCategory(Integer categoryId) {
    Category category = findCategory(categoryId);
    if (!category.getBooks().isEmpty()) {
      throw new HomeLibraryException(
              HttpStatus.CONFLICT, "Category cannot be deleted because it contains books");
    }
    category.getSubcategories().forEach(subcategoryRepository::delete);
    categoryRepository.delete(category);
  }

  private Category findCategory(Integer categoryId) {
    return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new HomeLibraryException(
                    HttpStatus.NOT_FOUND, "Category with given ID does not exist"));
  }
}
