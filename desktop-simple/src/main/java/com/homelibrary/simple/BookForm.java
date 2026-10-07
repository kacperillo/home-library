package com.homelibrary.simple;

import com.homelibrary.simple.Api.Book;
import com.homelibrary.simple.Api.Category;
import com.homelibrary.simple.Api.Priority;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

// Formularz książki pokazywany w JOptionPane. Przy edycji tytuł i autorzy są
// tylko do odczytu, bo API pozwala zmienić wyłącznie subkategorię i priorytet.
class BookForm extends JPanel {

  private final List<Category> categories;
  private final boolean editing;
  private final JTextField titleField = new JTextField(30);
  private final JTextField authorsField = new JTextField(30);
  private final JComboBox<Item> categoryBox = new JComboBox<>();
  private final JComboBox<Item> subcategoryBox = new JComboBox<>();
  private final JComboBox<Priority> priorityBox = new JComboBox<>(Priority.values());

  BookForm(List<Category> categories, Book book) {
    super(new GridLayout(0, 2, 8, 6));
    this.categories = categories;
    this.editing = book != null;

    categoryBox.setModel(new DefaultComboBoxModel<>(categories.stream().map(Item::of).toArray(Item[]::new)));
    categoryBox.addActionListener(e -> fillSubcategories());

    if (editing) {
      titleField.setText(book.title());
      titleField.setEditable(false);
      authorsField.setText(String.join(", ", book.authors()));
      authorsField.setEditable(false);
      select(categoryBox, book.categoryId());
      fillSubcategories();
      select(subcategoryBox, book.subcategoryId());
      priorityBox.setSelectedItem(Priority.values()[book.priority()]);
    } else {
      fillSubcategories();
      priorityBox.setSelectedItem(Priority.MEDIUM);
    }

    addRow("Tytuł:", titleField);
    addRow("Autorzy (po przecinku):", authorsField);
    addRow("Kategoria:", categoryBox);
    addRow("Subkategoria:", subcategoryBox);
    addRow("Priorytet:", priorityBox);
  }

  // Pokazuje formularz w oknie modalnym; przy błędzie walidacji wraca do formularza
  // z zachowanymi wartościami. Zwraca true, gdy użytkownik zatwierdził poprawne dane.
  boolean show(Component parent, String title) {
    while (true) {
      int choice = JOptionPane.showConfirmDialog(
          parent, this, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
      if (choice != JOptionPane.OK_OPTION) {
        return false;
      }
      String error = validateInput();
      if (error == null) {
        return true;
      }
      JOptionPane.showMessageDialog(parent, error, "Błąd", JOptionPane.ERROR_MESSAGE);
    }
  }

  String title() {
    return titleField.getText().trim();
  }

  List<String> authors() {
    return Arrays.stream(authorsField.getText().split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  int subcategoryId() {
    return ((Item) subcategoryBox.getSelectedItem()).id();
  }

  Priority priority() {
    return (Priority) priorityBox.getSelectedItem();
  }

  private String validateInput() {
    if (!editing && title().isEmpty()) {
      return "Podaj tytuł.";
    }
    if (!editing && authors().isEmpty()) {
      return "Podaj co najmniej jednego autora.";
    }
    if (subcategoryBox.getSelectedItem() == null) {
      return "Wybierz subkategorię.";
    }
    return null;
  }

  private void fillSubcategories() {
    Item category = (Item) categoryBox.getSelectedItem();
    Item[] items = categories.stream()
        .filter(c -> category != null && c.categoryId() == category.id())
        .flatMap(c -> c.subcategories().stream())
        .map(Item::of)
        .toArray(Item[]::new);
    subcategoryBox.setModel(new DefaultComboBoxModel<>(items));
  }

  private void addRow(String label, JComponent field) {
    add(new JLabel(label));
    add(field);
  }

  private static void select(JComboBox<Item> box, int id) {
    for (int i = 0; i < box.getItemCount(); i++) {
      if (Objects.equals(box.getItemAt(i).id(), id)) {
        box.setSelectedIndex(i);
        return;
      }
    }
  }
}
