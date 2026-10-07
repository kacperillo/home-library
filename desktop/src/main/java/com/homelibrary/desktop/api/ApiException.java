package com.homelibrary.desktop.api;

// Komunikat jest gotowy do pokazania użytkownikowi (pasek błędu, etykieta w dialogu).
public class ApiException extends Exception {

  public ApiException(String message) {
    super(message);
  }

  public ApiException(String message, Throwable cause) {
    super(message, cause);
  }
}
