package com.homelibrary.desktop.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

// Odpowiednik frontend/src/api.js: wywołania HTTP i ujednolicenie komunikatów błędów.
// Metody blokują wątek, więc wywołuje się je poza EDT (patrz ui.Async).
public class ApiClient {

  private final String baseUrl;
  private final HttpClient http;
  private final ObjectMapper mapper;

  public ApiClient(String baseUrl) {
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    this.mapper = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public List<Category> getCategories() throws ApiException {
    return request("GET", "/categories", null, new TypeReference<List<Category>>() {});
  }

  public BookPage getBooks(
      int pageNo,
      int pageSize,
      String sortParam,
      String sortDir,
      Integer categoryId,
      List<Integer> subcategoryIds) throws ApiException {
    StringBuilder query = new StringBuilder("/books?pageNo=").append(pageNo)
        .append("&pageSize=").append(pageSize)
        .append("&sortParam=").append(sortParam)
        .append("&sortDir=").append(sortDir);
    if (categoryId != null) {
      query.append("&categoryId=").append(categoryId);
    }
    for (Integer id : subcategoryIds) {
      query.append("&subcategoryIds=").append(id);
    }
    return request("GET", query.toString(), null, new TypeReference<BookPage>() {});
  }

  public Book addBook(NewBook book) throws ApiException {
    return request("POST", "/books", book, new TypeReference<Book>() {});
  }

  public Book updateBook(int bookId, BookUpdate update) throws ApiException {
    return request("PUT", "/books/" + bookId, update, new TypeReference<Book>() {});
  }

  public void deleteBook(int bookId) throws ApiException {
    request("DELETE", "/books/" + bookId, null, null);
  }

  private <T> T request(String method, String path, Object body, TypeReference<T> type) throws ApiException {
    HttpRequest.BodyPublisher publisher;
    try {
      publisher = body == null
          ? HttpRequest.BodyPublishers.noBody()
          : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8);
    } catch (JsonProcessingException e) {
      throw new ApiException("Nie udało się przygotować żądania: " + e.getOriginalMessage(), e);
    }

    HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + path))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .timeout(Duration.ofSeconds(15))
        .method(method, publisher)
        .build();

    HttpResponse<String> response;
    try {
      response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new ApiException("Brak połączenia z API. Czy backend działa pod adresem " + baseUrl + "?", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException("Żądanie zostało przerwane", e);
    }

    int status = response.statusCode();
    if (status == 204) {
      return null;
    }

    JsonNode json = parse(response.body());
    if (status < 200 || status >= 300) {
      throw new ApiException(errorMessage(json, status));
    }
    if (type == null || json == null) {
      return null;
    }
    return mapper.convertValue(json, type);
  }

  private JsonNode parse(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    try {
      return mapper.readTree(text);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  // API zwraca błędy w dwóch formatach: własnym ({message}) dla wyjątków
  // aplikacji i domyślnym Springa ({error, status}) dla reszty.
  private static String errorMessage(JsonNode body, int status) {
    if (body != null) {
      if (body.hasNonNull("message")) {
        return body.get("message").asText();
      }
      if (body.hasNonNull("error")) {
        return body.get("error").asText();
      }
    }
    return "Błąd " + status;
  }
}
