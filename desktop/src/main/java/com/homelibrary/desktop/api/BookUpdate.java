package com.homelibrary.desktop.api;

// Treść PUT /books/{id} — API pozwala zmienić tylko subkategorię i priorytet.
public record BookUpdate(int subcategoryId, int priority) {}
