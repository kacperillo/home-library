package com.homelibrary.desktop;

import java.util.List;

public final class Constants {

  // Wartości liczbowe muszą odpowiadać enumowi Priority po stronie API:
  // ZERO(0), LOW(1), MEDIUM(2), HIGH(3).
  public static final List<Option<Integer>> PRIORITIES = List.of(
      new Option<>(0, "ZERO"),
      new Option<>(1, "LOW"),
      new Option<>(2, "MEDIUM"),
      new Option<>(3, "HIGH"));

  public static final int DEFAULT_PRIORITY = 2; // MEDIUM

  public static final List<Integer> PAGE_SIZES = List.of(10, 20, 50, 100);

  public static final List<Option<String>> SORT_FIELDS = List.of(
      new Option<>("title", "Tytuł"),
      new Option<>("priority", "Priorytet"));

  public static final List<Option<String>> SORT_DIRECTIONS = List.of(
      new Option<>("asc", "Rosnąco"),
      new Option<>("desc", "Malejąco"));

  public static String priorityLabel(int value) {
    for (Option<Integer> priority : PRIORITIES) {
      if (priority.value() == value) {
        return priority.label();
      }
    }
    return String.valueOf(value);
  }

  private Constants() {}
}
