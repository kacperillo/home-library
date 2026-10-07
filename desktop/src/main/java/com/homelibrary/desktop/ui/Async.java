package com.homelibrary.desktop.ui;

import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

// Wywołania API blokują, więc idą przez SwingWorker; oba callbacki wracają na EDT.
final class Async {

  interface Task<T> {
    T run() throws Exception;
  }

  static <T> void run(Task<T> task, Consumer<T> onSuccess, Consumer<String> onError) {
    new SwingWorker<T, Void>() {
      @Override
      protected T doInBackground() throws Exception {
        return task.run();
      }

      @Override
      protected void done() {
        T result;
        try {
          result = get();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          onError.accept("Operacja została przerwana");
          return;
        } catch (ExecutionException e) {
          Throwable cause = e.getCause() == null ? e : e.getCause();
          onError.accept(cause.getMessage() == null ? cause.toString() : cause.getMessage());
          return;
        }
        onSuccess.accept(result);
      }
    }.execute();
  }

  private Async() {}
}
