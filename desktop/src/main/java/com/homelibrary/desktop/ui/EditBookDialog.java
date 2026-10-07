package com.homelibrary.desktop.ui;

import com.homelibrary.desktop.Constants;
import com.homelibrary.desktop.Option;
import com.homelibrary.desktop.api.ApiClient;
import com.homelibrary.desktop.api.Book;
import com.homelibrary.desktop.api.BookUpdate;
import com.homelibrary.desktop.api.Category;
import com.homelibrary.desktop.api.Subcategory;

import javax.swing.JComboBox;
import java.awt.Window;
import java.util.List;

// Zmiana subkategorii i priorytetu — odpowiednik EditBookModal.jsx.
// Do API trafia tylko subcategoryId i priority — wybór kategorii służy
// wyłącznie do zawężenia listy subkategorii.
class EditBookDialog extends FormDialog {

  private static final Option<Integer> CHOOSE = new Option<>(null, "— wybierz —");

  private final ApiClient api;
  private final Book book;
  private final List<Category> categories;
  private final Runnable onSaved;

  private final JComboBox<Option<Integer>> categoryCombo = new JComboBox<>();
  private final JComboBox<Option<Integer>> subcategoryCombo = new JComboBox<>();
  private final JComboBox<Option<Integer>> priorityCombo = Ui.combo(Constants.PRIORITIES);

  EditBookDialog(Window owner, ApiClient api, Book book, List<Category> categories, Runnable onSaved) {
    super(owner, "Edycja: " + book.title(), "Anuluj", "Zapisz zmiany");
    this.api = api;
    this.book = book;
    this.categories = categories;
    this.onSaved = onSaved;

    for (Category category : categories) {
      categoryCombo.addItem(new Option<>(category.categoryId(), category.categoryName()));
    }
    Ui.select(categoryCombo, book.categoryId());
    fillSubcategories(book.categoryId());
    Ui.select(subcategoryCombo, book.subcategoryId());
    categoryCombo.addActionListener(e -> handleCategoryChange());

    Ui.select(priorityCombo, book.priority());

    addRow("Kategoria", categoryCombo);
    addRow("Subkategoria", subcategoryCombo);
    addRow("Priorytet", priorityCombo);
    finishLayout();
  }

  private void handleCategoryChange() {
    Integer categoryId = Ui.selectedValue(categoryCombo);
    fillSubcategories(categoryId);
    // Dotychczasowa subkategoria należy do innej kategorii, więc wybór trzeba
    // zacząć od nowa — od pierwszej subkategorii nowej kategorii, jeśli jakaś jest.
    List<Subcategory> subcategories = subcategoriesOf(categoryId);
    Ui.select(subcategoryCombo, subcategories.isEmpty() ? null : subcategories.get(0).subcategoryId());
  }

  private void fillSubcategories(Integer categoryId) {
    subcategoryCombo.removeAllItems();
    subcategoryCombo.addItem(CHOOSE);
    for (Subcategory subcategory : subcategoriesOf(categoryId)) {
      subcategoryCombo.addItem(new Option<>(subcategory.subcategoryId(), subcategory.subcategoryName()));
    }
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

  @Override
  protected void submit() {
    setError(null);
    Integer subcategoryId = Ui.selectedValue(subcategoryCombo);
    if (subcategoryId == null) {
      setError("Pole subkategorii jest puste");
      return;
    }
    int priority = Ui.selectedValue(priorityCombo);

    setBusy(true);
    BookUpdate update = new BookUpdate(subcategoryId, priority);
    Async.run(() -> api.updateBook(book.bookId(), update), saved -> {
      dispose();
      onSaved.run();
    }, message -> {
      setError(message);
      setBusy(false);
    });
  }
}
