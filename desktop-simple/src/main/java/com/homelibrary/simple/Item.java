package com.homelibrary.simple;

import com.homelibrary.simple.Api.Category;
import com.homelibrary.simple.Api.Subcategory;

// Pozycja JComboBox: identyfikator wysyłany do API i etykieta, którą combo
// wyświetla przez toString(). id == null oznacza „wszystkie”.
record Item(Integer id, String label) {

  static Item of(Category category) {
    return new Item(category.categoryId(), category.categoryName());
  }

  static Item of(Subcategory subcategory) {
    return new Item(subcategory.subcategoryId(), subcategory.subcategoryName());
  }

  @Override
  public String toString() {
    return label;
  }
}
