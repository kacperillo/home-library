package com.homelibrary.simple;

import com.homelibrary.simple.Api.Book;
import com.homelibrary.simple.Api.Category;
import com.homelibrary.simple.Api.Page;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MainFrame extends JFrame {

  private record Sort(String param, String dir, String label) {
    @Override
    public String toString() {
      return label;
    }
  }

  private static final Item ALL = new Item(null, "Wszystkie");
  private static final Item WIDTH_PROTOTYPE = new Item(0, "Elektrotechnika i energetyka");
  private static final Sort[] SORTS = {
      new Sort("title", "asc", "Tytuł A–Z"),
      new Sort("title", "desc", "Tytuł Z–A"),
      new Sort("priority", "desc", "Priorytet malejąco"),
      new Sort("priority", "asc", "Priorytet rosnąco"),
  };
  private static final Integer[] PAGE_SIZES = {10, 20, 50, 100};

  private final Api api;
  private List<Category> categories = List.of();
  private int pageNo;

  private final JComboBox<Item> categoryBox = new JComboBox<>(new Item[] {ALL});
  private final MultiSelect subcategorySelect = new MultiSelect(() -> reload(0));
  private final JComboBox<Sort> sortBox = new JComboBox<>(SORTS);
  private final JComboBox<Integer> pageSizeBox = new JComboBox<>(PAGE_SIZES);
  private final JButton editButton = new JButton("Edytuj");
  private final JButton deleteButton = new JButton("Usuń");
  private final JButton previousButton = new JButton("‹");
  private final JButton nextButton = new JButton("›");
  private final JLabel pageLabel = new JLabel();
  private final BookTableModel model = new BookTableModel();
  private final JTable table = new JTable(model);

  public MainFrame(Api api) {
    super("Home Library");
    this.api = api;
    setDefaultCloseOperation(EXIT_ON_CLOSE);

    JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
    filters.add(new JLabel("Kategoria:"));
    filters.add(categoryBox);
    filters.add(new JLabel("Subkategorie:"));
    filters.add(subcategorySelect);
    filters.add(new JLabel("Sortowanie:"));
    filters.add(sortBox);
    JButton addButton = new JButton("Dodaj");
    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    actions.add(addButton);
    actions.add(editButton);
    actions.add(deleteButton);
    JPanel top = new JPanel(new BorderLayout());
    top.add(filters, BorderLayout.WEST);
    top.add(actions, BorderLayout.EAST);

    JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
    bottom.add(previousButton);
    bottom.add(pageLabel);
    bottom.add(nextButton);
    bottom.add(new JLabel("Na stronie:"));
    bottom.add(pageSizeBox);

    categoryBox.setPrototypeDisplayValue(WIDTH_PROTOTYPE);
    subcategorySelect.setPreferredSize(categoryBox.getPreferredSize());
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    editButton.setEnabled(false);
    deleteButton.setEnabled(false);
    pageSizeBox.setSelectedItem(20);

    add(top, BorderLayout.NORTH);
    add(new JScrollPane(table), BorderLayout.CENTER);
    add(bottom, BorderLayout.SOUTH);

    categoryBox.addActionListener(e -> {
      subcategorySelect.setOptions(subcategoryItems());
      reload(0);
    });
    sortBox.addActionListener(e -> reload(0));
    pageSizeBox.addActionListener(e -> reload(0));
    previousButton.addActionListener(e -> reload(pageNo - 1));
    nextButton.addActionListener(e -> reload(pageNo + 1));

    addButton.addActionListener(e -> addBook());
    editButton.addActionListener(e -> editSelected());
    deleteButton.addActionListener(e -> deleteSelected());
    table.getSelectionModel().addListSelectionListener(e -> {
      boolean selected = table.getSelectedRow() >= 0;
      editButton.setEnabled(selected);
      deleteButton.setEnabled(selected);
    });
    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && selectedBook() != null) {
          editSelected();
        }
      }
    });

    setSize(1000, 600);
    setLocationRelativeTo(null);
  }

  public void start() {
    run(api::categories, result -> {
      categories = result;
      Item[] items = Stream.concat(Stream.of(ALL), result.stream().map(Item::of)).toArray(Item[]::new);
      categoryBox.setModel(new DefaultComboBoxModel<>(items));
    });
    reload(0);
  }

  private void reload(int page) {
    Item category = (Item) categoryBox.getSelectedItem();
    List<Integer> subcategoryIds = subcategorySelect.getSelectedIds();
    Sort sort = (Sort) sortBox.getSelectedItem();
    int pageSize = (Integer) pageSizeBox.getSelectedItem();

    run(() -> api.books(page, pageSize, sort.param(), sort.dir(), category.id(), subcategoryIds), result -> {
      // Po usunięciu ostatniego rekordu na ostatniej stronie numer strony wychodzi poza zakres.
      if (page > 0 && page >= result.totalPages()) {
        reload(result.totalPages() - 1);
        return;
      }
      pageNo = page;
      show(result);
    });
  }

  private void show(Page result) {
    model.setBooks(result.content());
    pageLabel.setText("Strona %d z %d (książek: %d)".formatted(
        pageNo + 1, Math.max(result.totalPages(), 1), result.totalElements()));
    previousButton.setEnabled(pageNo > 0);
    nextButton.setEnabled(pageNo < result.totalPages() - 1);
  }

  private void addBook() {
    BookForm form = new BookForm(categories, null);
    if (form.show(this, "Nowa książka")) {
      run(() -> api.addBook(form.title(), form.authors(), form.subcategoryId(), form.priority()), () -> reload(pageNo));
    }
  }

  private void editSelected() {
    Book book = selectedBook();
    BookForm form = new BookForm(categories, book);
    if (form.show(this, "Edycja: " + book.title())) {
      run(() -> api.updateBook(book.bookId(), form.subcategoryId(), form.priority()), () -> reload(pageNo));
    }
  }

  private void deleteSelected() {
    Book book = selectedBook();
    int choice = JOptionPane.showConfirmDialog(this, "Usunąć książkę „" + book.title() + "”?",
        "Usuwanie książki", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if (choice == JOptionPane.YES_OPTION) {
      run(() -> api.deleteBook(book.bookId()), () -> reload(pageNo));
    }
  }

  private Book selectedBook() {
    int row = table.getSelectedRow();
    return row < 0 ? null : model.getBook(row);
  }

  private List<Item> subcategoryItems() {
    Item category = (Item) categoryBox.getSelectedItem();
    return categories.stream()
        .filter(c -> category.id() != null && c.categoryId() == category.id())
        .flatMap(c -> c.subcategories().stream())
        .map(Item::of)
        .toList();
  }

  private interface Action {
    void run() throws Exception;
  }

  private void run(Action action, Runnable onDone) {
    run(() -> {
      action.run();
      return null;
    }, ignored -> onDone.run());
  }

  // Wywołanie API w tle (SwingWorker); wynik wraca na wątek GUI, błąd trafia do okienka.
  private <T> void run(Callable<T> call, Consumer<T> onDone) {
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    new SwingWorker<T, Void>() {
      @Override
      protected T doInBackground() throws Exception {
        return call.call();
      }

      @Override
      protected void done() {
        setCursor(Cursor.getDefaultCursor());
        T result;
        try {
          result = get();
        } catch (Exception e) {
          Throwable cause = e.getCause() == null ? e : e.getCause();
          JOptionPane.showMessageDialog(MainFrame.this, cause.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
          return;
        }
        onDone.accept(result);
      }
    }.execute();
  }
}
