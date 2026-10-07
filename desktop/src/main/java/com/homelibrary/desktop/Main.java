package com.homelibrary.desktop;

import com.homelibrary.desktop.api.ApiClient;
import com.homelibrary.desktop.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {

  private static final String DEFAULT_URL = "http://localhost:8080";

  public static void main(String[] args) {
    String baseUrl = resolveBaseUrl(args);
    SwingUtilities.invokeLater(() -> {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (Exception ignored) {
      }
      MainFrame frame = new MainFrame(new ApiClient(baseUrl));
      frame.setVisible(true);
      frame.start();
    });
  }

  // Adres API: argument programu > -Dhomelibrary.url > zmienna HOME_LIBRARY_URL > localhost:8080.
  private static String resolveBaseUrl(String[] args) {
    if (args.length > 0 && !args[0].isBlank()) {
      return args[0];
    }
    String property = System.getProperty("homelibrary.url");
    if (property != null && !property.isBlank()) {
      return property;
    }
    String env = System.getenv("HOME_LIBRARY_URL");
    if (env != null && !env.isBlank()) {
      return env;
    }
    return DEFAULT_URL;
  }

  private Main() {}
}
