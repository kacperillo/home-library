package com.homelibrary.model;

public enum Priority {
  ZERO(0),
  LOW(1),
  MEDIUM(2),
  HIGH(3);

  public static final Priority defaultPriority = Priority.LOW;
  private final int value;

  Priority(int value) {
    this.value = value;
  }

  public static Priority fromValue(int value) throws IllegalArgumentException {
    for (Priority priority : Priority.values()) {
      if (value == priority.getValue()) {
        return priority;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

  public int getValue() {
    return value;
  }
}
