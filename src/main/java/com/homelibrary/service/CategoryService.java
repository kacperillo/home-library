package com.homelibrary.service;

import com.homelibrary.api.request.CategoryRequest;
import com.homelibrary.api.request.UpdateNameRequest;
import com.homelibrary.api.response.CategoryResponse;
import com.homelibrary.exception.HomeLibraryException;
import com.homelibrary.model.Category;
import com.homelibrary.repository.BookRepository;
import com.homelibrary.repository.CategoryRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

  private final CategoryRepository categoryRepository;
  private final BookRepository bookRepository;

  public CategoryResponse addCategory(@Valid CategoryRequest request) {
    Category category = new Category(request.getCategoryName());
    category = categoryRepository.save(category);
    return new CategoryResponse(category);
  }

  public List<CategoryResponse> getAllCategories() {
    List<Category> categories = categoryRepository.findAll();
    List<CategoryResponse> categoryResponseList = new ArrayList<>();
    categories.forEach(category -> categoryResponseList.add(new CategoryResponse(category)));
    return categoryResponseList;
  }

  public CategoryResponse updateCategoryName(Integer categoryId, UpdateNameRequest request) {
    Category category = findCategory(categoryId);
    if (category.getName().equals(request.getUpdatedName())) {
      throw new HomeLibraryException(HttpStatus.BAD_REQUEST, "Category name is the same");
    }
    category.setName(request.getUpdatedName());
    category = categoryRepository.save(category);
    return new CategoryResponse(category);
  }

  public void deleteCategory(Integer categoryId) {
    if (!categoryRepository.existsById(categoryId)) {
      throw new HomeLibraryException(HttpStatus.NOT_FOUND, "Category with given ID does not exist");
    }

    if (bookRepository.existsBySubcategoryCategoryId(categoryId)) {
      throw new HomeLibraryException(HttpStatus.CONFLICT, "Category cannot be deleted because it contains books");
    }
    categoryRepository.deleteById(categoryId);
  }

  private Category findCategory(Integer categoryId) {
    return categoryRepository.findById(categoryId)
        .orElseThrow(() -> new HomeLibraryException(
            HttpStatus.NOT_FOUND, "Category with given ID does not exist"));
  }
}
