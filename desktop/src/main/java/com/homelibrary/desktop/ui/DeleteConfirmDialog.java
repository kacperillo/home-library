package com.homelibrary.desktop.ui;

import com.homelibrary.desktop.api.ApiClient;
import com.homelibrary.desktop.api.Book;

import javax.swing.JLabel;
import java.awt.Font;
import java.awt.Window;

// Potwierdzenie usunięcia — odpowiednik DeleteConfirmModal.jsx.
class DeleteConfirmDialog extends FormDialog {

  private final ApiClient api;
  private final Book book;
  private final Runnable onDeleted;

  DeleteConfirmDialog(Window owner, ApiClient api, Book book, Runnable onDeleted) {
    super(owner, "Usuwanie książki", "Nie", "Tak");
    this.api = api;
    this.book = book;
    this.onDeleted = onDeleted;

    getPrimaryButton().setForeground(Ui.ERROR_COLOR);

    JLabel title = new JLabel(book.title());
    title.setFont(title.getFont().deriveFont(Font.BOLD));
    addRow(new JLabel("Czy na pewno chcesz usunąć ten rekord?"));
    addRow(title);
    finishLayout();
  }

  @Override
  protected void submit() {
    setBusy(true);
    setError(null);
    Async.run(() -> {
      api.deleteBook(book.bookId());
      return null;
    }, ignored -> {
      dispose();
      onDeleted.run();
    }, message -> {
      setError(message);
      setBusy(false);
    });
  }
}
