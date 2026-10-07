package com.homelibrary.desktop.ui;

import com.homelibrary.desktop.api.ApiClient;
import com.homelibrary.desktop.api.Book;
import com.homelibrary.desktop.api.BookPage;
import com.homelibrary.desktop.api.Category;
import com.homelibrary.desktop.api.Subcategory;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.List;

// Stan widoku, pobieranie danych i składanie całości — odpowiednik App.jsx.
public class MainFrame extends JFrame {

  private static final String CARD_LOADING = "loading";
  private static final String CARD_EMPTY = "empty";
  private static final String CARD_TABLE = "table";

  private final ApiClient api;

  private List<Category> categories = List.of();
  private Integer categoryId; // null = wszystkie
  private List<Integer> selectedSubcategoryIds = List.of();
  private String sortParam = "title";
  private String sortDir = "asc";
  private int pageNo = 0;
  private int pageSize = 20;
  private BookPage page;

  private final JLabel errorBar = new JLabel();
  private final TopBar topBar;
  private final BooksTable booksTable;
  private final BottomBar bottomBar;
  private final CardLayout centerLayout = new CardLayout();
  private final JPanel center = new JPanel(centerLayout);

  public MainFrame(ApiClient api) {
    super("Home Library");
    this.api = api;
    setDefaultCloseOperation(EXIT_ON_CLOSE);

    topBar = new TopBar(new TopBar.Listener() {
      @Override
      public void categoryChanged(Integer value) {
        categoryId = value;
        selectedSubcategoryIds = List.of();
        topBar.setSubcategories(subcategoriesOf(value), value != null);
        pageNo = 0;
        loadBooks();
      }

      @Override
      public void subcategoriesChanged(List<Integer> value) {
        selectedSubcategoryIds = value;
        pageNo = 0;
        loadBooks();
      }

      @Override
      public void sortParamChanged(String value) {
        sortParam = value;
        pageNo = 0;
        loadBooks();
      }

      @Override
      public void sortDirChanged(String value) {
        sortDir = value;
        pageNo = 0;
        loadBooks();
      }

      @Override
      public void addClicked() {
        new AddBookDialog(MainFrame.this, api, categories, MainFrame.this::loadBooks).setVisible(true);
      }
    });

    booksTable = new BooksTable(
        book -> new EditBookDialog(this, api, book, categories, this::loadBooks).setVisible(true),
        book -> new DeleteConfirmDialog(this, api, book, this::loadBooks).setVisible(true));

    bottomBar = new BottomBar(new BottomBar.Listener() {
      @Override
      public void pageChanged(int value) {
        pageNo = value;
        loadBooks();
      }

      @Override
      public void pageSizeChanged(int value) {
        pageSize = value;
        pageNo = 0;
        loadBooks();
      }
    }, pageSize);

    JLabel heading = new JLabel("Home Library");
    heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
    heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

    errorBar.setOpaque(true);
    errorBar.setBackground(new Color(0xFD, 0xEC, 0xEA));
    errorBar.setForeground(Ui.ERROR_COLOR);
    errorBar.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(0xF5, 0xC6, 0xC0)),
        BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    errorBar.setVisible(false);

    JPanel north = new JPanel();
    north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
    north.setOpaque(false);
    for (JComponent component : List.of(heading, errorBar, topBar)) {
      component.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    }
    north.add(heading);
    north.add(errorBar);
    north.add(topBar);

    center.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 1, Ui.BORDER_COLOR));
    center.add(message("Wczytywanie…"), CARD_LOADING);
    center.add(message("Brak książek do wyświetlenia."), CARD_EMPTY);
    center.add(booksTable, CARD_TABLE);

    JPanel root = new JPanel(new BorderLayout());
    root.setBackground(new Color(0xF5, 0xF5, 0xF5));
    root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    root.add(north, BorderLayout.NORTH);
    root.add(center, BorderLayout.CENTER);
    root.add(bottomBar, BorderLayout.SOUTH);
    setContentPane(root);

    // Okno nie może być większe niż obszar roboczy ekranu (istotne przy skalowaniu DPI).
    Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    setSize(Math.min(1200, screen.width - 40), Math.min(700, screen.height - 40));
    setLocationRelativeTo(null);
  }

  // Pierwsze pobranie danych po pokazaniu okna.
  public void start() {
    loadCategories();
    loadBooks();
  }

  private void loadCategories() {
    Async.run(api::getCategories, result -> {
      categories = result;
      topBar.setCategories(result);
    }, this::setError);
  }

  private void loadBooks() {
    setError(null);
    centerLayout.show(center, CARD_LOADING);

    int requestedPageNo = pageNo;
    int requestedPageSize = pageSize;
    String requestedSortParam = sortParam;
    String requestedSortDir = sortDir;
    Integer requestedCategoryId = categoryId;
    List<Integer> requestedSubcategoryIds = selectedSubcategoryIds;

    Async.run(
        () -> api.getBooks(requestedPageNo, requestedPageSize, requestedSortParam, requestedSortDir,
            requestedCategoryId, requestedSubcategoryIds),
        result -> {
          page = result;
          // Po usunięciu ostatniego rekordu na ostatniej stronie numer strony
          // wykracza poza zakres — cofamy się i pobieramy ponownie.
          if (result.totalPages() > 0 && pageNo > result.totalPages() - 1) {
            pageNo = result.totalPages() - 1;
            loadBooks();
            return;
          }
          render();
        },
        message -> {
          page = null;
          setError(message);
          render();
        });
  }

  private void render() {
    List<Book> books = page == null ? List.of() : page.content();
    booksTable.setBooks(books, pageNo * pageSize);
    centerLayout.show(center, books.isEmpty() ? CARD_EMPTY : CARD_TABLE);
    bottomBar.update(
        pageNo,
        page == null ? 0 : page.totalPages(),
        page == null ? 0 : page.totalElements());
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

  private void setError(String message) {
    errorBar.setText(message);
    errorBar.setVisible(message != null);
    errorBar.getParent().revalidate();
  }

  private static JLabel message(String text) {
    JLabel label = new JLabel(text, SwingConstants.CENTER);
    label.setOpaque(true);
    label.setBackground(Color.WHITE);
    label.setForeground(Ui.LABEL_COLOR);
    return label;
  }
}
