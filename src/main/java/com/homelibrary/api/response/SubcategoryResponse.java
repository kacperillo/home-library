package com.homelibrary.api.response;

import com.homelibrary.model.Subcategory;
import lombok.Builder;
import lombok.Getter;

@Getter
public class SubcategoryResponse {

  private final Integer subcategoryId;
  private final String subcategoryName;

  public SubcategoryResponse(Subcategory subcategory) {
    subcategoryId = subcategory.getId();
    subcategoryName = subcategory.getName();
  }
}
