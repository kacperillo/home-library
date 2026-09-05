package com.homelibrary.api.response;

import com.homelibrary.model.Book;
import com.homelibrary.model.Category;
import com.homelibrary.model.Subcategory;
import lombok.Getter;

import java.util.List;

@Getter
public class BookResponse {

  private final Integer bookId;
  private final String title;
  private final List<String> authors;
  private final Integer categoryId;
  private final String categoryName;
  private final Integer subcategoryId;
  private final String subcategoryName;
  private final Integer priority;

  public BookResponse(Book book) {
    bookId = book.getId();
    title = book.getTitle();
    authors = book.getAuthors();
    Category category = book.getCategory();
    categoryId = category.getId();
    categoryName = category.getName();
    Subcategory subcategory = book.getSubcategory();
    subcategoryId = subcategory.getId();
    subcategoryName = subcategory.getName();
    priority = book.getPriority().getValue();
  }
}
