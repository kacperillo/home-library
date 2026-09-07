package com.homelibrary.controller;

import com.homelibrary.api.request.BookRequest;
import com.homelibrary.api.request.BookUpdateRequest;
import com.homelibrary.api.response.BookResponse;
import com.homelibrary.api.response.BookResponsePage;
import com.homelibrary.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

  private final BookService bookService;

  @PostMapping
  public ResponseEntity<BookResponse> addBook(@Valid @RequestBody BookRequest bookRequest) {
    BookResponse bookResponse = bookService.addBook(bookRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(bookResponse);
  }

  @GetMapping
  public ResponseEntity<BookResponsePage> getAllBooks(
          @RequestParam(required = false, defaultValue = "0") int pageNo,
          @RequestParam(required = false, defaultValue = "20") int pageSize,
          @RequestParam(required = false, defaultValue = "title") String sortParam,
          @RequestParam(required = false, defaultValue = "asc") String sortDir,
          @RequestParam(required = false) Integer categoryId) {
    BookResponsePage bookResponsePage = bookService.getAllBooks(pageNo, pageSize, sortParam, sortDir, categoryId);
    return ResponseEntity.ok().body(bookResponsePage);
  }

  @GetMapping("/{bookId}")
  public ResponseEntity<BookResponse> getBook(@PathVariable Integer bookId) {
    BookResponse bookResponse = bookService.getBook(bookId);
    return ResponseEntity.ok().body(bookResponse);
  }

  @PutMapping("/{bookId}")
  public ResponseEntity<BookResponse> updateBook(
          @PathVariable Integer bookId, @RequestBody BookUpdateRequest bookUpdateRequest) {
    BookResponse bookResponse = bookService.updateBook(bookId, bookUpdateRequest);
    return ResponseEntity.ok().body(bookResponse);
  }

  @DeleteMapping("/{bookId}")
  public ResponseEntity<Void> deleteBook(@PathVariable Integer bookId) {
    bookService.deleteBook(bookId);
    return ResponseEntity.noContent().build();
  }
}
