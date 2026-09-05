package com.homelibrary.api.response;

import com.homelibrary.model.Category;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

@Getter
public class CategoryResponse {

  private final Integer categoryId;
  private final String categoryName;
  private final List<SubcategoryResponse> subcategories;

  public CategoryResponse(Category category) {
    categoryId = category.getId();
    categoryName = category.getName();
    subcategories = getSubcategories(category);
  }

  private List<SubcategoryResponse> getSubcategories(Category category) {
    List<SubcategoryResponse> subcategoryResponseList = new ArrayList<>();
    category.getSubcategories().forEach(subcategory -> {
      SubcategoryResponse subcategoryResponse = new SubcategoryResponse(subcategory);
      subcategoryResponseList.add(subcategoryResponse);
    });
    return subcategoryResponseList;
  }
}
