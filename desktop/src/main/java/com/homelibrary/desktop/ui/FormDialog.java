package com.homelibrary.desktop.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.KeyEvent;

// Wspólna ramka okna modalnego — odpowiednik Modal.jsx plus przyciski akcji
// i etykieta błędu, które w React każdy dialog składał osobno.
abstract class FormDialog extends JDialog {

  private static final int WIDTH = 420;

  private final JPanel body = new JPanel();
  private final JLabel errorLabel = Ui.errorLabel();
  private final JButton cancelButton;
  private final JButton primaryButton;

  FormDialog(Window owner, String title, String cancelText, String primaryText) {
    super(owner, title, ModalityType.APPLICATION_MODAL);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

    cancelButton = new JButton(cancelText);
    cancelButton.addActionListener(e -> dispose());
    primaryButton = new JButton(primaryText);
    primaryButton.addActionListener(e -> submit());

    JPanel actions = new JPanel();
    actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
    actions.add(Box.createHorizontalGlue());
    actions.add(cancelButton);
    actions.add(Box.createHorizontalStrut(8));
    actions.add(primaryButton);

    JPanel south = new JPanel();
    south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
    errorLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    actions.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    south.add(errorLabel);
    south.add(Box.createVerticalStrut(12));
    south.add(actions);

    JPanel root = new JPanel(new BorderLayout(0, 12));
    root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
    root.add(body, BorderLayout.CENTER);
    root.add(south, BorderLayout.SOUTH);
    setContentPane(root);

    getRootPane().setDefaultButton(primaryButton);
    getRootPane().registerKeyboardAction(
        e -> dispose(),
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        JRootPane.WHEN_IN_FOCUSED_WINDOW);
  }

  protected abstract void submit();

  protected void addRow(String label, JComponent component) {
    addRow(Ui.field(label, component));
  }

  protected void addRow(JComponent component) {
    component.setAlignmentX(JComponent.LEFT_ALIGNMENT);
    if (body.getComponentCount() > 0) {
      body.add(Box.createVerticalStrut(10));
    }
    body.add(component);
  }

  protected JButton getPrimaryButton() {
    return primaryButton;
  }

  protected void setError(String message) {
    errorLabel.setText(message == null ? " " : message);
    pack();
  }

  // Na czas żądania do API oba przyciski są nieaktywne, tak jak `busy` w React.
  protected void setBusy(boolean busy) {
    cancelButton.setEnabled(!busy);
    primaryButton.setEnabled(!busy);
  }

  // Wywoływane po dodaniu wszystkich wierszy: rozmiar i pozycja względem właściciela.
  protected void finishLayout() {
    pack();
    Dimension size = getSize();
    size.width = Math.max(size.width, WIDTH);
    setSize(size);
    setMinimumSize(size);
    setLocationRelativeTo(getOwner());
  }
}
