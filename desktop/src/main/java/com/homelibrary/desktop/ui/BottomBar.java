package com.homelibrary.desktop.ui;

import com.homelibrary.desktop.Constants;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

// Stronicowanie i liczba pozycji na stronie — odpowiednik BottomBar.jsx.
// pageNo z API jest liczone od zera, użytkownikowi pokazujemy numery od jedynki.
class BottomBar extends JPanel {

  interface Listener {
    void pageChanged(int pageNo);

    void pageSizeChanged(int pageSize);
  }

  private final Listener listener;
  private final JButton previousButton = new JButton("‹ Poprzednia");
  private final JButton nextButton = new JButton("Następna ›");
  private final JTextField pageInput = new JTextField(4);
  private final JLabel totalPagesLabel = new JLabel("z 0");
  private final JComboBox<Integer> pageSizeCombo = new JComboBox<>();
  private final JLabel totalElementsLabel = new JLabel("Wszystkich książek: 0");

  private int pageNo;
  private int totalPages;

  BottomBar(Listener listener, int initialPageSize) {
    this.listener = listener;
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    setBackground(Color.WHITE);
    setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 1, 1, 1, Ui.BORDER_COLOR),
        BorderFactory.createEmptyBorder(10, 12, 10, 12)));

    previousButton.addActionListener(e -> listener.pageChanged(pageNo - 1));
    nextButton.addActionListener(e -> listener.pageChanged(pageNo + 1));

    pageInput.addActionListener(e -> commitInput());
    pageInput.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        commitInput();
      }
    });
    Ui.fixWidth(pageInput, 56);

    for (Integer size : Constants.PAGE_SIZES) {
      pageSizeCombo.addItem(size);
    }
    pageSizeCombo.setSelectedItem(initialPageSize);
    pageSizeCombo.addActionListener(e -> {
      Integer size = (Integer) pageSizeCombo.getSelectedItem();
      if (size != null) {
        listener.pageSizeChanged(size);
      }
    });
    Ui.fixWidth(pageSizeCombo, 80);

    add(previousButton);
    add(Box.createHorizontalStrut(12));
    add(new JLabel("Strona"));
    add(Box.createHorizontalStrut(6));
    add(pageInput);
    add(Box.createHorizontalStrut(6));
    add(totalPagesLabel);
    add(Box.createHorizontalStrut(12));
    add(nextButton);
    add(Box.createHorizontalStrut(16));
    add(Ui.fixedField("Na stronie", pageSizeCombo));
    add(Box.createHorizontalGlue());
    add(totalElementsLabel);
  }

  void update(int pageNo, int totalPages, int totalElements) {
    this.pageNo = pageNo;
    this.totalPages = totalPages;
    pageInput.setText(String.valueOf(pageNo + 1));
    totalPagesLabel.setText("z " + totalPages);
    totalElementsLabel.setText("Wszystkich książek: " + totalElements);
    previousButton.setEnabled(pageNo > 0);
    nextButton.setEnabled(totalPages > 0 && pageNo < totalPages - 1);
  }

  private void commitInput() {
    int parsed;
    try {
      parsed = Integer.parseInt(pageInput.getText().trim());
    } catch (NumberFormatException e) {
      pageInput.setText(String.valueOf(pageNo + 1));
      return;
    }
    int lastPage = Math.max(totalPages, 1);
    int clamped = Math.min(Math.max(parsed, 1), lastPage);
    pageInput.setText(String.valueOf(clamped));
    if (clamped - 1 != pageNo) {
      listener.pageChanged(clamped - 1);
    }
  }
}
