package com.homelibrary.simple;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Przycisk rozwijający panel z checkboxami — Swing nie ma combo z wielokrotnym wyborem.
// Checkbox nie jest JMenuItem, więc kliknięcie w niego nie zamyka panelu; zamyka go
// dopiero kliknięcie poza nim. Puste zaznaczenie oznacza „wszystkie”.
class MultiSelect extends JButton {

  private final Runnable onChange;
  private List<Item> options = List.of();
  private final Set<Integer> selectedIds = new LinkedHashSet<>();

  MultiSelect(Runnable onChange) {
    this.onChange = onChange;
    setHorizontalAlignment(LEFT);
    addActionListener(e -> showPopup());
    refreshText();
  }

  void setOptions(List<Item> options) {
    this.options = options;
    selectedIds.clear();
    refreshText();
  }

  List<Integer> getSelectedIds() {
    return List.copyOf(selectedIds);
  }

  private void showPopup() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    if (options.isEmpty()) {
      panel.add(new JLabel("Najpierw wybierz kategorię"));
    }
    for (Item option : options) {
      JCheckBox box = new JCheckBox(option.label(), selectedIds.contains(option.id()));
      box.addActionListener(e -> {
        if (!selectedIds.remove(option.id())) {
          selectedIds.add(option.id());
        }
        refreshText();
        onChange.run();
      });
      panel.add(box);
    }
    JPopupMenu popup = new JPopupMenu();
    popup.add(panel);
    popup.show(this, 0, getHeight());
  }

  private void refreshText() {
    setText((selectedIds.isEmpty() ? "Wszystkie" : "Wybrano: " + selectedIds.size()) + " ▾");
  }
}
