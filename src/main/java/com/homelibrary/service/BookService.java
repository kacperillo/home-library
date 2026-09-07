package com.homelibrary.service;

import com.homelibrary.api.request.BookRequest;
import com.homelibrary.api.request.BookUpdateRequest;
import com.homelibrary.api.response.BookResponse;
import com.homelibrary.api.response.BookResponsePage;
import com.homelibrary.exception.HomeLibraryException;
import com.homelibrary.model.*;
import com.homelibrary.repository.AuthorRepository;
import com.homelibrary.repository.BookRepository;
import com.homelibrary.repository.SubcategoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class BookService {

  private final BookRepository bookRepository;
  private final SubcategoryRepository subcategoryRepository;
  private final AuthorRepository authorRepository;

  public BookResponse addBook(BookRequest request) {
    Subcategory subcategory = findSubcategory(request.getSubcategoryId());
    Priority priority = request.getPriority() != null ? findPriority(request.getPriority()) : Priority.defaultPriority;

    List<Author> authors = findOrCreateAuthors(request.getAuthors());

    Book book = Book.builder()
        .title(request.getTitle())
        .authors(authors)
        .subcategory(subcategory)
        .priority(priority)
        .build();

    addBookToAuthors(book, authors);
    book = bookRepository.save(book);
    return new BookResponse(book);
  }

  public BookResponse getBook(Integer bookId) {
    Book book = findBook(bookId);
    return new BookResponse(book);
  }

  public BookResponsePage getAllBooks(
      int pageNo, int pageSize, String sortParam, String sortDir, Integer categoryId) {
    Sort sort = Sort.by(sortParam);
    sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
        ? sort.ascending()
        : sort.descending();
    Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
    Page<Book> bookPage;
    if (categoryId != null) {
      bookPage = bookRepository.findBySubcategoryCategoryId(categoryId, pageable);
    } else {
      bookPage = bookRepository.findAll(pageable);
    }
    List<Book> books = bookPage.getContent();
      return BookResponsePage.builder()
              .pageNo(pageNo)
              .pageSize(bookPage.getSize())
              .totalPages(bookPage.getTotalPages())
              .totalElements((int) bookPage.getTotalElements())
              .content(books.stream().map(BookResponse::new).toList())
              .build();
  }

  public BookResponse updateBook(Integer bookId, BookUpdateRequest request) {
    Book book = findBook(bookId);

    if (book.getSubcategory().getId().equals(request.getSubcategoryId()) &&
        book.getPriority().getValue() == request.getPriority()) {
      throw new HomeLibraryException(HttpStatus.BAD_REQUEST, "No change detected");
    }

    if (!book.getSubcategory().getId().equals(request.getSubcategoryId())) {
      Subcategory newSubcategory = findSubcategory(request.getSubcategoryId());
      book.setSubcategory(newSubcategory);
    }

    if (book.getPriority().getValue() != request.getPriority()) {
      Priority newPriority = findPriority(request.getPriority());
      book.setPriority(newPriority);
    }

    book = bookRepository.save(book);
    return new BookResponse(book);
  }

  public void deleteBook(Integer bookId) {
    Book book = findBook(bookId);
    removeBookFromAuthors(book, book.getAuthors());
    deleteAuthorsIfNoBooks(book.getAuthors());
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

  private List<Author> findOrCreateAuthors(List<String> authorsNames) {
    return authorsNames.stream().map(this::findOrCreateAuthor).toList();
  }

  private Author findOrCreateAuthor(String name) {
    return authorRepository.findByName(name)
        .orElseGet(() -> authorRepository.save(new Author(name)));
  }

  private void addBookToAuthors(Book book, List<Author> authors) {
    authors.forEach(author -> author.addBook(book));
  }

  private void removeBookFromAuthors(Book book, List<Author> authors) {
    authors.forEach(author -> author.removeBook(book));
  }

  private void deleteAuthorsIfNoBooks(List<Author> authors) {
    authors.stream().filter(author -> author.getBooks().isEmpty())
        .forEach(authorRepository::delete);
  }
}
