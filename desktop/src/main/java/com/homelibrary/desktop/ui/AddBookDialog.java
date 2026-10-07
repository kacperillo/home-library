package com.homelibrary.desktop.ui;

import com.homelibrary.desktop.Constants;
import com.homelibrary.desktop.Option;
import com.homelibrary.desktop.api.ApiClient;
import com.homelibrary.desktop.api.Category;
import com.homelibrary.desktop.api.NewBook;
import com.homelibrary.desktop.api.Subcategory;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

// Formularz nowej książki — odpowiednik AddBookModal.jsx.
class AddBookDialog extends FormDialog {

  private static final Option<Integer> CHOOSE = new Option<>(null, "— wybierz —");

  private final ApiClient api;
  private final List<Category> categories;
  private final Runnable onSaved;

  private final JTextField titleField = new JTextField();
  private final JPanel authorsPanel = new JPanel();
  private final List<JTextField> authorFields = new ArrayList<>();
  private final JComboBox<Option<Integer>> categoryCombo = new JComboBox<>();
  private final JComboBox<Option<Integer>> subcategoryCombo = new JComboBox<>();
  private final JComboBox<Option<Integer>> priorityCombo = Ui.combo(Constants.PRIORITIES);

  AddBookDialog(Window owner, ApiClient api, List<Category> categories, Runnable onSaved) {
    super(owner, "Nowa książka", "Anuluj", "Dodaj");
    this.api = api;
    this.categories = categories;
    this.onSaved = onSaved;

    categoryCombo.addItem(CHOOSE);
    for (Category category : categories) {
      categoryCombo.addItem(new Option<>(category.categoryId(), category.categoryName()));
    }
    categoryCombo.addActionListener(e -> handleCategoryChange());

    subcategoryCombo.addItem(CHOOSE);
    subcategoryCombo.setEnabled(false);

    Ui.select(priorityCombo, Constants.DEFAULT_PRIORITY);

    authorsPanel.setLayout(new BoxLayout(authorsPanel, BoxLayout.Y_AXIS));
    authorsPanel.setOpaque(false);
    authorFields.add(new JTextField());
    JButton addAuthorButton = new JButton("+");
    addAuthorButton.addActionListener(e -> addAuthorField());
    rebuildAuthorRows();

    JPanel authorsBlock = new JPanel();
    authorsBlock.setLayout(new BoxLayout(authorsBlock, BoxLayout.Y_AXIS));
    authorsBlock.setOpaque(false);
    authorsPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    addAuthorButton.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    authorsBlock.add(authorsPanel);
    authorsBlock.add(Box.createVerticalStrut(4));
    authorsBlock.add(addAuthorButton);

    addRow("Tytuł", titleField);
    addRow("Autorzy", authorsBlock);
    addRow("Kategoria", categoryCombo);
    addRow("Subkategoria", subcategoryCombo);
    addRow("Priorytet", priorityCombo);
    finishLayout();
  }

  private void handleCategoryChange() {
    Integer categoryId = Ui.selectedValue(categoryCombo);
    subcategoryCombo.removeAllItems();
    subcategoryCombo.addItem(CHOOSE);
    for (Subcategory subcategory : subcategoriesOf(categoryId)) {
      subcategoryCombo.addItem(new Option<>(subcategory.subcategoryId(), subcategory.subcategoryName()));
    }
    subcategoryCombo.setEnabled(categoryId != null);
  }

  private List<Subcategory> subcategoriesOf(Integer categoryId) {
    if (categoryId == null) {
      return List.of();
    }
    for (Category category : categories) {
      if (category.categoryId() == categoryId) {
        return category.subcategories();
      }
    }
    return List.of();
  }

  private void addAuthorField() {
    // Kolejne pole ma sens dopiero wtedy, gdy poprzednie zostało wypełnione.
    if (authorFields.get(authorFields.size() - 1).getText().isBlank()) {
      setError("Pole autora jest puste");
      return;
    }
    setError(null);
    JTextField field = new JTextField();
    authorFields.add(field);
    rebuildAuthorRows();
    field.requestFocusInWindow();
  }

  private void removeAuthorField(JTextField field) {
    if (authorFields.size() == 1) {
      return;
    }
    authorFields.remove(field);
    rebuildAuthorRows();
  }

  private void rebuildAuthorRows() {
    authorsPanel.removeAll();
    for (JTextField field : authorFields) {
      if (authorsPanel.getComponentCount() > 0) {
        authorsPanel.add(Box.createVerticalStrut(4));
      }
      JPanel row = new JPanel(new BorderLayout(4, 0));
      row.setOpaque(false);
      row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
      field.setToolTipText("Imię i nazwisko");
      row.add(field, BorderLayout.CENTER);
      if (authorFields.size() > 1) {
        JButton remove = new JButton("−");
        remove.addActionListener(e -> removeAuthorField(field));
        row.add(remove, BorderLayout.EAST);
      }
      authorsPanel.add(row);
    }
    authorsPanel.revalidate();
    authorsPanel.repaint();
    pack();
  }

  @Override
  protected void submit() {
    String title = titleField.getText().trim();
    if (title.isEmpty()) {
      setError("Pole tytułu jest puste");
      return;
    }
    List<String> authors = new ArrayList<>();
    for (JTextField field : authorFields) {
      String author = field.getText().trim();
      if (author.isEmpty()) {
        setError("Pole autora jest puste");
        return;
      }
      authors.add(author);
    }
    if (Ui.selectedValue(categoryCombo) == null) {
      setError("Pole kategorii jest puste");
      return;
    }
    Integer subcategoryId = Ui.selectedValue(subcategoryCombo);
    if (subcategoryId == null) {
      setError("Pole subkategorii jest puste");
      return;
    }
    int priority = Ui.selectedValue(priorityCombo);

    setError(null);
    setBusy(true);
    NewBook book = new NewBook(title, authors, subcategoryId, priority);
    Async.run(() -> api.addBook(book), saved -> {
      dispose();
      onSaved.run();
    }, message -> {
      setError(message);
      setBusy(false);
    });
  }
}
