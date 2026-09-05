package com.homelibrary.service;

import com.homelibrary.api.request.BookRequest;
import com.homelibrary.api.request.BookUpdateRequest;
import com.homelibrary.api.response.BookResponse;
import com.homelibrary.exception.HomeLibraryException;
import com.homelibrary.model.*;
import com.homelibrary.repository.BookRepository;
import com.homelibrary.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

  private final BookRepository bookRepository;
  private final SubcategoryRepository subcategoryRepository;

  public BookResponse addBook(BookRequest bookRequest) {
    Subcategory subcategory = findSubcategory(bookRequest.getSubcategoryId());
    Priority priority = bookRequest.getPriority() != null ?
            findPriority(bookRequest.getPriority()) : Priority.defaultPriority;

    Book book = Book.builder()
            .title(bookRequest.getTitle())
            .authors(bookRequest.getAuthors())
            .subcategory(subcategory)
            .priority(priority)
            .build();

    book = bookRepository.save(book);
    return new BookResponse(book);
  }

  public BookResponse getBook(Integer bookId) {
    Book book = findBook(bookId);
    return new BookResponse(book);
  }

  public List<BookResponse> getAllBooks(
          int pageNo, int pageSize, String sortParam, String sortDir, Integer priorityValue) {
    Sort sort = Sort.by(sortParam);
    sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
            ? sort.ascending() : sort.descending();
    Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
    Page<Book> bookPage;
    if (priorityValue != null) {
      try {
        Priority priority = Priority.fromValue(priorityValue);
        bookPage = bookRepository.findByPriority(priority, pageable);
      } catch (IllegalArgumentException ex) {
        throw new HomeLibraryException(HttpStatus.BAD_REQUEST, "Invalid priority");
      }
    } else {
       bookPage = bookRepository.findAll(pageable);
    }
    List<Book> books = bookPage.getContent();
    List<BookResponse> bookListResponse = new ArrayList<>();
    books.forEach(book -> bookListResponse.add(new BookResponse(book)));
    return bookListResponse;
  }

  public BookResponse updateBook(Integer bookId, BookUpdateRequest bookUpdateRequest) {
    Book book = findBook(bookId);

    if (book.getSubcategory().getId() == bookUpdateRequest.getSubcategoryId() &&
        book.getPriority().getValue() == bookUpdateRequest.getPriority()) {
      throw new HomeLibraryException(HttpStatus.CONFLICT, "No change detected");
    }

    if (book.getSubcategory().getId() != bookUpdateRequest.getSubcategoryId()) {
      Subcategory newSubcategory = findSubcategory(bookUpdateRequest.getSubcategoryId());
      book.setSubcategory(newSubcategory);
    }

    if (book.getPriority().getValue() != bookUpdateRequest.getPriority()) {
      Priority newPriority = findPriority(bookUpdateRequest.getPriority());
      book.setPriority(newPriority);
    }

    book = bookRepository.save(book);
    return new BookResponse(book);
  }

  public void deleteBook(Integer bookId) {
    Book book = findBook(bookId);
    bookRepository.delete(book);
  }

  private Book findBook(Integer bookId) {
    return bookRepository.findById(bookId)
            .orElseThrow(() -> new HomeLibraryException(
                    HttpStatus.NOT_FOUND, "Book with given ID does not exist"));
  }

  private Subcategory findSubcategory(Integer subcategoryId) {
    return subcategoryRepository.findById(subcategoryId)
            .orElseThrow(() -> new HomeLibraryException(
                    HttpStatus.NOT_FOUND, "Subcategory with given ID does not exist"));
  }

  private Priority findPriority(int value) {
    try {
      return Priority.fromValue(value);
    } catch (IllegalArgumentException e) {
      throw new HomeLibraryException(HttpStatus.BAD_REQUEST, "Invalid priority");
    }
  }
}
