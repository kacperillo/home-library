package com.homelibrary.controller;

import com.homelibrary.api.request.CategoryRequest;
import com.homelibrary.api.request.UpdateNameRequest;
import com.homelibrary.api.response.CategoryResponse;
import com.homelibrary.service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;

  @PostMapping
  public ResponseEntity<CategoryResponse> addCategory(@Valid @RequestBody CategoryRequest categoryRequest) {
    CategoryResponse categoryResponse = categoryService.addCategory(categoryRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(categoryResponse);
  }

  @GetMapping
  public ResponseEntity<List<CategoryResponse>> getAllCategories() {
    List<CategoryResponse> categoryResponseList = categoryService.getAllCategories();
    return ResponseEntity.ok().body(categoryResponseList);
  }

  @PutMapping("/{categoryId}")
  public ResponseEntity<CategoryResponse> updateCategoryName(
      @PathVariable Integer categoryId, @RequestBody UpdateNameRequest updateNameRequest) {
    CategoryResponse categoryResponse = categoryService.updateCategoryName(categoryId, updateNameRequest);
    return ResponseEntity.ok().body(categoryResponse);
  }

  @DeleteMapping("/{categoryId}")
  public ResponseEntity<Void> deleteCategory(@PathVariable Integer categoryId) {
    categoryService.deleteCategory(categoryId);
    return ResponseEntity.noContent().build();
  }
}
