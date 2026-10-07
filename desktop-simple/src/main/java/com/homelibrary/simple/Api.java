package com.homelibrary.simple;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;

// Klient REST API home-library. Metody blokują — wywołuj je poza wątkiem GUI.
public class Api {

  public record Book(int bookId, String title, List<String> authors, int categoryId, String categoryName,
                     int subcategoryId, String subcategoryName, int priority) {}

  public record Page(List<Book> content, int totalElements, int totalPages) {}

  public record Category(int categoryId, String categoryName, List<Subcategory> subcategories) {}

  public record Subcategory(int subcategoryId, String subcategoryName) {}

  // Kolejność musi odpowiadać enumowi Priority w API: wartość w JSON to ordinal().
  public enum Priority { ZERO, LOW, MEDIUM, HIGH }

  private final String baseUrl;
  private final HttpClient http = HttpClient.newHttpClient();
  private final ObjectMapper json = JsonMapper.builder()
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .build();

  public Api(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public List<Category> categories() throws IOException, InterruptedException {
    return json.readValue(send("GET", "/categories", null), new TypeReference<>() {});
  }

  public Page books(int pageNo, int pageSize, String sortParam, String sortDir,
                    Integer categoryId, List<Integer> subcategoryIds) throws IOException, InterruptedException {
    String query = "?pageNo=%d&pageSize=%d&sortParam=%s&sortDir=%s".formatted(pageNo, pageSize, sortParam, sortDir);
    if (categoryId != null) {
      query += "&categoryId=" + categoryId;
    }
    for (Integer id : subcategoryIds) {
      query += "&subcategoryIds=" + id;
    }
    return json.readValue(send("GET", "/books" + query, null), Page.class);
  }

  public void addBook(String title, List<String> authors, int subcategoryId, Priority priority)
      throws IOException, InterruptedException {
    send("POST", "/books", Map.of(
        "title", title, "authors", authors, "subcategoryId", subcategoryId, "priority", priority.ordinal()));
  }

  public void updateBook(int bookId, int subcategoryId, Priority priority) throws IOException, InterruptedException {
    send("PUT", "/books/" + bookId, Map.of("subcategoryId", subcategoryId, "priority", priority.ordinal()));
  }

  public void deleteBook(int bookId) throws IOException, InterruptedException {
    send("DELETE", "/books/" + bookId, null);
  }

  private String send(String method, String path, Object body) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
        .header("Content-Type", "application/json")
        .method(method, body == null ? BodyPublishers.noBody() : BodyPublishers.ofString(json.writeValueAsString(body)))
        .build();
    HttpResponse<String> response;
    try {
      response = http.send(request, BodyHandlers.ofString());
    } catch (ConnectException e) {
      throw new IOException("Brak połączenia z API pod adresem " + baseUrl, e);
    }
    if (response.statusCode() >= 400) {
      throw new IOException(errorMessage(response));
    }
    return response.body();
  }

  // API zwraca błędy jako {message} (wyjątki aplikacji) albo {error, status} (domyślnie Spring).
  private String errorMessage(HttpResponse<String> response) {
    try {
      JsonNode node = json.readTree(response.body());
      for (String field : List.of("message", "error")) {
        if (node.hasNonNull(field)) {
          return node.get(field).asText();
        }
      }
    } catch (IOException ignored) {
      // treść nie jest JSON-em — zostaje sam kod odpowiedzi
    }
    return "HTTP " + response.statusCode();
  }
}
