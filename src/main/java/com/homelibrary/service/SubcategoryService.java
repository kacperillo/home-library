package com.homelibrary.service;

import com.homelibrary.api.request.SubcategoryRequest;
import com.homelibrary.api.request.UpdateNameRequest;
import com.homelibrary.api.response.SubcategoryResponse;
import com.homelibrary.exception.HomeLibraryException;
import com.homelibrary.model.Category;
import com.homelibrary.model.Subcategory;
import com.homelibrary.repository.BookRepository;
import com.homelibrary.repository.CategoryRepository;
import com.homelibrary.repository.SubcategoryRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class SubcategoryService {

  private final CategoryRepository categoryRepository;
  private final SubcategoryRepository subcategoryRepository;
  private final BookRepository bookRepository;

  public SubcategoryResponse addSubcategory(SubcategoryRequest request) {
    Category category = findCategory(request.getCategoryId());
    Subcategory subcategory = new Subcategory(request.getSubcategoryName(), category);
    subcategory = subcategoryRepository.save(subcategory);
    return new SubcategoryResponse(subcategory);
  }

  public SubcategoryResponse updateSubcategoryName(Integer subcategoryId, @Valid UpdateNameRequest request) {
    Subcategory subcategory = findSubcategory(subcategoryId);
    if (subcategory.getName().equals(request.getUpdatedName())) {
      throw new HomeLibraryException(HttpStatus.BAD_REQUEST, "Subcategory name is the same");
    }
    subcategory.setName(request.getUpdatedName());
    subcategory = subcategoryRepository.save(subcategory);
    return new SubcategoryResponse(subcategory);
  }

  public void deleteSubcategory(Integer subcategoryId) {
    if (!subcategoryRepository.existsById(subcategoryId)) {
      throw new HomeLibraryException(HttpStatus.NOT_FOUND, "Subcategory with given ID does not exist");
    }

    if (bookRepository.existsBySubcategoryId(subcategoryId)) {
      throw new HomeLibraryException(HttpStatus.CONFLICT, "Subcategory cannot be deleted because it contains books");
    }
    subcategoryRepository.deleteById(subcategoryId);
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
