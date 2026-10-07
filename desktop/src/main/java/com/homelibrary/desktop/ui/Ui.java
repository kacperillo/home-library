package com.homelibrary.desktop.ui;

import com.homelibrary.desktop.Option;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

final class Ui {

  static final Color LABEL_COLOR = new Color(0x55, 0x55, 0x55);
  static final Color ERROR_COLOR = new Color(0xB0, 0x00, 0x20);
  static final Color BORDER_COLOR = new Color(0xDD, 0xDD, 0xDD);

  static JPanel field(String label, JComponent component) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setOpaque(false);
    JLabel caption = new JLabel(label);
    caption.setForeground(LABEL_COLOR);
    caption.setFont(caption.getFont().deriveFont(Font.PLAIN, 11f));
    caption.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    component.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    panel.add(caption);
    panel.add(Box.createVerticalStrut(3));
    panel.add(component);
    return panel;
  }

  static <T> JComboBox<Option<T>> combo(List<Option<T>> options) {
    JComboBox<Option<T>> combo = new JComboBox<>();
    for (Option<T> option : options) {
      combo.addItem(option);
    }
    return combo;
  }

  // Ustawia pozycję o danej wartości (null wybiera pozycję-zaślepkę, jeśli istnieje).
  static <T> void select(JComboBox<Option<T>> combo, T value) {
    for (int i = 0; i < combo.getItemCount(); i++) {
      Option<T> item = combo.getItemAt(i);
      if (value == null ? item.value() == null : value.equals(item.value())) {
        combo.setSelectedIndex(i);
        return;
      }
    }
  }

  static <T> T selectedValue(JComboBox<Option<T>> combo) {
    int index = combo.getSelectedIndex();
    return index < 0 ? null : combo.getItemAt(index).value();
  }

  static JLabel errorLabel() {
    JLabel label = new JLabel(" ");
    label.setForeground(ERROR_COLOR);
    return label;
  }

  static void fixWidth(JComponent component, int width) {
    Dimension size = component.getPreferredSize();
    size.width = width;
    component.setPreferredSize(size);
    component.setMinimumSize(size);
    component.setMaximumSize(new Dimension(width, size.height));
  }

  // W paskach (BoxLayout w poziomie) pole nie ma rosnąć wraz z oknem — wolne
  // miejsce ma trafić do createHorizontalGlue().
  static JPanel fixedField(String label, JComponent component) {
    JPanel panel = field(label, component);
    panel.setMaximumSize(panel.getPreferredSize());
    return panel;
  }

  private Ui() {}
}
