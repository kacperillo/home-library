package com.homelibrary.controller;

import com.homelibrary.api.response.CategoryResponse;
import com.homelibrary.api.response.SubcategoryResponse;
import com.homelibrary.service.CategoryService;

import com.homelibrary.service.SubcategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;
  private final SubcategoryService subcategoryService;

  @PostMapping
  public ResponseEntity<CategoryResponse> addCategory(@Valid @RequestBody String categoryName) {
    CategoryResponse categoryResponse = categoryService.addCategory(categoryName);
    return ResponseEntity.status(HttpStatus.CREATED).body(categoryResponse);
  }

  @GetMapping
  public ResponseEntity<List<CategoryResponse>> getAllCategories() {
    List<CategoryResponse> categoryResponseList = categoryService.getAllCategories();
    return ResponseEntity.ok().body(categoryResponseList);
  }

  @PutMapping("/categories/{categoryId}")
  public ResponseEntity<CategoryResponse> updateCategoryName(
      @PathVariable Integer categoryId, @RequestBody String newCategoryName) {
    CategoryResponse categoryResponse = categoryService.updateCategoryName(categoryId, newCategoryName);
    return ResponseEntity.ok().body(categoryResponse);
  }

  @DeleteMapping("/categories/{categoryId}")
  public ResponseEntity<Void> deleteCategory(@PathVariable Integer categoryId) {
    categoryService.deleteCategory(categoryId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/categories/{categoryId}/subcategories")
  public ResponseEntity<SubcategoryResponse> addSubcategory(
          @PathVariable Integer categoryId, @Valid @RequestBody String subcategoryName) {
    SubcategoryResponse subcategoryResponse = subcategoryService.addSubcategory(categoryId, subcategoryName);
    return ResponseEntity.status(HttpStatus.CREATED).body(subcategoryResponse);
  }

  @PutMapping("/subcategories/{subcategoryId}")
  public ResponseEntity<SubcategoryResponse> updateSubcategoryName(
          @PathVariable Integer subcategoryId, @Valid @RequestBody String newSubcategoryName) {
    SubcategoryResponse subcategoryResponse =
            subcategoryService.updateSubcategoryName(subcategoryId, newSubcategoryName);
    return ResponseEntity.ok().body(subcategoryResponse);
  }

  @DeleteMapping("/subcategories/{subcategoriesId}")
  public ResponseEntity<Void> deleteSubcategory(@PathVariable Integer subcategoryId) {
    subcategoryService.deleteSubcategory(subcategoryId);
    return ResponseEntity.noContent().build();
  }
}
