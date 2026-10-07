package com.homelibrary.desktop.api;

import java.util.List;

public record BookPage(List<Book> content, int pageNo, int pageSize, int totalElements, int totalPages) {}
