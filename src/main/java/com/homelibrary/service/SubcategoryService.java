package com.homelibrary.service;

import com.homelibrary.api.response.SubcategoryResponse;
import com.homelibrary.exception.HomeLibraryException;
import com.homelibrary.model.Category;
import com.homelibrary.model.Subcategory;
import com.homelibrary.repository.CategoryRepository;
import com.homelibrary.repository.SubcategoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Setter
public class SubcategoryService {

  private final CategoryRepository categoryRepository;
  private final SubcategoryRepository subcategoryRepository;

  public SubcategoryResponse addSubcategory(Integer categoryId, String subcategoryName) {
    Category category = findCategory(categoryId);
    Subcategory subcategory = new Subcategory(subcategoryName, category);
    subcategory = subcategoryRepository.save(subcategory);
    return new SubcategoryResponse(subcategory);
  }

  public SubcategoryResponse updateSubcategoryName(Integer subcategoryId, String newSubcategoryName) {
    Subcategory subcategory = findSubcategory(subcategoryId);
    if (subcategory.getName() == newSubcategoryName) {
      throw new HomeLibraryException(HttpStatus.CONFLICT, "Subcategory name is the same");
    }
    subcategory.setName(newSubcategoryName);
    subcategory = subcategoryRepository.save(subcategory);
    return new SubcategoryResponse(subcategory);
  }

  public void deleteSubcategory(Integer subcategoryId) {
    Subcategory subcategory = findSubcategory(subcategoryId);
    if (!subcategory.getBooks().isEmpty()) {
      throw new HomeLibraryException(
              HttpStatus.CONFLICT, "Subcategory cannot be deleted because it contains books");
    }
    subcategoryRepository.delete(subcategory);
  }

  private Subcategory findSubcategory(Integer subcategoryId) {
    return subcategoryRepository.findById(subcategoryId)
            .orElseThrow(() -> new HomeLibraryException(
                    HttpStatus.NOT_FOUND, "Subcategory with given ID does not exist"));
  }

  private Category findCategory(Integer categoryId) {
    return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new HomeLibraryException(
                    HttpStatus.NOT_FOUND, "Category with given ID does not exist"));
  }
}
