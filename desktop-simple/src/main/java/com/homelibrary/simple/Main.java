package com.homelibrary.simple;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

  public static void main(String[] args) throws Exception {
    String baseUrl = args.length > 0 ? args[0] : "http://localhost:8080";
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    SwingUtilities.invokeLater(() -> {
      MainFrame frame = new MainFrame(new Api(baseUrl));
      frame.setVisible(true);
      frame.start();
    });
  }
}
