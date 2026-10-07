package com.homelibrary.desktop.ui;

import com.homelibrary.desktop.Constants;
import com.homelibrary.desktop.Option;
import com.homelibrary.desktop.api.Category;
import com.homelibrary.desktop.api.Subcategory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

class TopBar extends JPanel {

  interface Listener {
    void categoryChanged(Integer categoryId);

    void subcategoriesChanged(List<Integer> subcategoryIds);

    void sortParamChanged(String sortParam);

    void sortDirChanged(String sortDir);

    void addClicked();
  }

  private final JComboBox<Option<Integer>> categoryCombo = new JComboBox<>();
  private final MultiSelect subcategorySelect;
  private final JComboBox<Option<String>> sortParamCombo = Ui.combo(Constants.SORT_FIELDS);
  private final JComboBox<Option<String>> sortDirCombo = Ui.combo(Constants.SORT_DIRECTIONS);
  private final JButton addButton = new JButton("Dodaj");

  private boolean updating;

  TopBar(Listener listener) {
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    setBackground(Color.WHITE);
    setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(1, 1, 0, 1, Ui.BORDER_COLOR),
        BorderFactory.createEmptyBorder(10, 12, 10, 12)));

    subcategorySelect = new MultiSelect("Najpierw wybierz kategorię", listener::subcategoriesChanged);
    subcategorySelect.setEnabled(false);

    categoryCombo.addItem(new Option<>(null, "Wszystkie"));
    categoryCombo.addActionListener(e -> {
      if (!updating) {
        listener.categoryChanged(Ui.selectedValue(categoryCombo));
      }
    });
    sortParamCombo.addActionListener(e -> listener.sortParamChanged(Ui.selectedValue(sortParamCombo)));
    sortDirCombo.addActionListener(e -> listener.sortDirChanged(Ui.selectedValue(sortDirCombo)));
    addButton.addActionListener(e -> listener.addClicked());

    Ui.fixWidth(categoryCombo, 180);
    Ui.fixWidth(subcategorySelect, 170);
    Ui.fixWidth(sortParamCombo, 120);
    Ui.fixWidth(sortDirCombo, 120);

    add(Ui.fixedField("Kategoria", categoryCombo));
    add(Box.createHorizontalStrut(12));
    add(Ui.fixedField("Subkategorie", subcategorySelect));
    add(Box.createHorizontalStrut(12));
    add(Ui.fixedField("Sortuj po", sortParamCombo));
    add(Box.createHorizontalStrut(12));
    add(Ui.fixedField("Kierunek", sortDirCombo));
    add(Box.createHorizontalGlue());
    add(addButton);
  }

  void setCategories(List<Category> categories) {
    Integer previous = Ui.selectedValue(categoryCombo);
    updating = true;
    try {
      categoryCombo.removeAllItems();
      categoryCombo.addItem(new Option<>(null, "Wszystkie"));
      for (Category category : categories) {
        categoryCombo.addItem(new Option<>(category.categoryId(), category.categoryName()));
      }
      Ui.select(categoryCombo, previous);
    } finally {
      updating = false;
    }
  }

  // Lista jest pusta i nieaktywna, dopóki nie wybrano kategorii.
  void setSubcategories(List<Subcategory> subcategories, boolean categorySelected) {
    List<Option<Integer>> options = new ArrayList<>();
    for (Subcategory subcategory : subcategories) {
      options.add(new Option<>(subcategory.subcategoryId(), subcategory.subcategoryName()));
    }
    subcategorySelect.setOptions(options);
    subcategorySelect.setEnabled(categorySelected);
  }
}
