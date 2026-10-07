package com.homelibrary.simple;

import com.homelibrary.simple.Api.Book;
import com.homelibrary.simple.Api.Priority;

import javax.swing.table.AbstractTableModel;
import java.util.List;

class BookTableModel extends AbstractTableModel {

  private static final String[] COLUMNS = {"Tytuł", "Kategoria", "Subkategoria", "Autorzy", "Priorytet"};

  private List<Book> books = List.of();

  void setBooks(List<Book> books) {
    this.books = books;
    fireTableDataChanged();
  }

  Book getBook(int row) {
    return books.get(row);
  }

  @Override
  public int getRowCount() {
    return books.size();
  }

  @Override
  public int getColumnCount() {
    return COLUMNS.length;
  }

  @Override
  public String getColumnName(int column) {
    return COLUMNS[column];
  }

  @Override
  public Object getValueAt(int row, int column) {
    Book book = books.get(row);
    return switch (column) {
      case 0 -> book.title();
      case 1 -> book.categoryName();
      case 2 -> book.subcategoryName();
      case 3 -> String.join(", ", book.authors());
      case 4 -> Priority.values()[book.priority()];
      default -> throw new IllegalArgumentException("Nieznana kolumna " + column);
    };
  }
}
