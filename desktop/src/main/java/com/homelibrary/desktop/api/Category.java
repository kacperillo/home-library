package com.homelibrary.desktop.api;

import java.util.List;

public record Category(int categoryId, String categoryName, List<Subcategory> subcategories) {}
