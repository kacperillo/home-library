package com.homelibrary.desktop.api;

import java.util.List;

public record Book(
    int bookId,
    String title,
    List<String> authors,
    int categoryId,
    String categoryName,
    int subcategoryId,
    String subcategoryName,
    int priority) {}
