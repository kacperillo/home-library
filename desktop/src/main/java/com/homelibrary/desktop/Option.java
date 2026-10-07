package com.homelibrary.desktop;

// Pozycja listy rozwijanej: wartość wysyłana do API i etykieta widoczna dla użytkownika.
// JComboBox wyświetla toString(), więc etykieta jest tu jedyną reprezentacją tekstową.
public record Option<T>(T value, String label) {

  @Override
  public String toString() {
    return label;
  }
}
