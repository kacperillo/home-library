package com.homelibrary.desktop.ui;

import com.homelibrary.desktop.Constants;
import com.homelibrary.desktop.api.Book;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// Tabela z rekordami — odpowiednik BooksTable.jsx. Kolumna „Akcje” zawiera
// przyciski, więc jest jedyną kolumną „edytowalną”: edytor komórki to panel z przyciskami.
class BooksTable extends JPanel {

  private static final int ACTIONS_COLUMN = 6;

  private final Model model = new Model();
  private final JTable table = new JTable(model);

  BooksTable(Consumer<Book> onEdit, Consumer<Book> onDelete) {
    super(new BorderLayout());

    table.setFillsViewportHeight(true);
    table.setRowHeight(32);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getTableHeader().setReorderingAllowed(false);
    table.setAutoCreateRowSorter(false);

    TableColumnModel columns = table.getColumnModel();
    columns.getColumn(0).setMaxWidth(56);
    columns.getColumn(0).setPreferredWidth(48);
    columns.getColumn(1).setPreferredWidth(320);
    columns.getColumn(2).setPreferredWidth(140);
    columns.getColumn(3).setPreferredWidth(140);
    columns.getColumn(4).setPreferredWidth(220);
    columns.getColumn(5).setPreferredWidth(90);
    columns.getColumn(ACTIONS_COLUMN).setPreferredWidth(170);
    columns.getColumn(ACTIONS_COLUMN).setMinWidth(170);
    columns.getColumn(ACTIONS_COLUMN).setCellRenderer(new ActionsRenderer());
    columns.getColumn(ACTIONS_COLUMN).setCellEditor(new ActionsEditor(onEdit, onDelete));

    add(new JScrollPane(table), BorderLayout.CENTER);
  }

  void setBooks(List<Book> books, int ordinalOffset) {
    if (table.isEditing()) {
      table.getCellEditor().cancelCellEditing();
    }
    model.set(books, ordinalOffset);
  }

  private static class Model extends AbstractTableModel {

    private static final String[] COLUMNS =
        {"Lp.", "Tytuł", "Kategoria", "Subkategoria", "Autorzy", "Priorytet", "Akcje"};

    private List<Book> books = new ArrayList<>();
    private int ordinalOffset;

    void set(List<Book> books, int ordinalOffset) {
      this.books = new ArrayList<>(books);
      this.ordinalOffset = ordinalOffset;
      fireTableDataChanged();
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
    public boolean isCellEditable(int row, int column) {
      return column == ACTIONS_COLUMN;
    }

    @Override
    public Object getValueAt(int row, int column) {
      Book book = books.get(row);
      return switch (column) {
        case 0 -> ordinalOffset + row + 1;
        case 1 -> book.title();
        case 2 -> book.categoryName();
        case 3 -> book.subcategoryName();
        case 4 -> String.join(", ", book.authors());
        case 5 -> Constants.priorityLabel(book.priority());
        case ACTIONS_COLUMN -> book;
        default -> null;
      };
    }
  }

  // Wspólny wygląd komórki z przyciskami; renderer i edytor mają osobne
  // instancje, bo komponent Swinga może mieć tylko jednego rodzica.
  private static class ActionsPanel extends JPanel {

    final JButton editButton = new JButton("Edytuj");
    final JButton deleteButton = new JButton("Usuń");

    ActionsPanel() {
      super(new FlowLayout(FlowLayout.CENTER, 6, 2));
      editButton.setMargin(new java.awt.Insets(1, 8, 1, 8));
      deleteButton.setMargin(new java.awt.Insets(1, 8, 1, 8));
      deleteButton.setForeground(Ui.ERROR_COLOR);
      add(editButton);
      add(deleteButton);
    }

    void matchTable(JTable table, boolean selected) {
      setBackground(selected ? table.getSelectionBackground() : table.getBackground());
    }
  }

  private static class ActionsRenderer extends ActionsPanel implements TableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      matchTable(table, isSelected);
      return this;
    }
  }

  private static class ActionsEditor extends AbstractCellEditor implements TableCellEditor {

    private final ActionsPanel panel = new ActionsPanel();
    private Book current;

    ActionsEditor(Consumer<Book> onEdit, Consumer<Book> onDelete) {
      // Edycja komórki kończy się przed otwarciem dialogu, żeby tabela nie
      // została w stanie „w trakcie edycji” po odświeżeniu danych.
      panel.editButton.addActionListener(e -> {
        Book book = current;
        fireEditingStopped();
        onEdit.accept(book);
      });
      panel.deleteButton.addActionListener(e -> {
        Book book = current;
        fireEditingStopped();
        onDelete.accept(book);
      });
    }

    @Override
    public Component getTableCellEditorComponent(
        JTable table, Object value, boolean isSelected, int row, int column) {
      current = (Book) value;
      panel.matchTable(table, true);
      return panel;
    }

    @Override
    public Object getCellEditorValue() {
      return current;
    }
  }
}
