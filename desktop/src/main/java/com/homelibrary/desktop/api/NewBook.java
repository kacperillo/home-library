package com.homelibrary.desktop.api;

import java.util.List;

// Treść POST /books.
public record NewBook(String title, List<String> authors, int subcategoryId, int priority) {}
