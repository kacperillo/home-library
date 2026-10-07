package com.homelibrary.desktop.ui;

import com.homelibrary.desktop.Option;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;


class MultiSelect extends JButton {

  private static final int MAX_PANEL_HEIGHT = 240;

  private final String emptyText;
  private final Consumer<List<Integer>> onChange;
  private List<Option<Integer>> options = List.of();
  private final Set<Integer> selected = new LinkedHashSet<>();

  MultiSelect(String emptyText, Consumer<List<Integer>> onChange) {
    this.emptyText = emptyText;
    this.onChange = onChange;
    setHorizontalAlignment(LEFT);
    addActionListener(e -> showPanel());
    refreshText();
  }

  void setOptions(List<Option<Integer>> options) {
    this.options = List.copyOf(options);
    selected.clear();
    refreshText();
  }

  List<Integer> getSelected() {
    return new ArrayList<>(selected);
  }

  private void showPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

    if (options.isEmpty()) {
      JLabel empty = new JLabel(emptyText);
      empty.setForeground(Ui.LABEL_COLOR);
      panel.add(empty);
    }
    for (Option<Integer> option : options) {
      JCheckBox box = new JCheckBox(option.label(), selected.contains(option.value()));
      box.setOpaque(false);
      box.addActionListener(e -> toggle(option.value()));
      panel.add(box);
    }

    JScrollPane scroll = new JScrollPane(panel);
    scroll.setBorder(null);
    Dimension size = panel.getPreferredSize();
    size.width = Math.max(size.width + 24, getWidth());
    size.height = Math.min(size.height + 4, MAX_PANEL_HEIGHT);
    scroll.setPreferredSize(size);

    // Pola wyboru nie są elementami menu, więc kliknięcie w nie nie zamyka
    // panelu — zamyka go dopiero kliknięcie poza nim.
    JPopupMenu popup = new JPopupMenu();
    popup.add(scroll);
    popup.show(this, 0, getHeight());
  }

  private void toggle(Integer value) {
    if (!selected.remove(value)) {
      selected.add(value);
    }
    refreshText();
    onChange.accept(getSelected());
  }

  private void refreshText() {
    String summary = selected.isEmpty() ? "Wszystkie" : "Wybrano: " + selected.size();
    setText(summary + " ▾");
  }
}
