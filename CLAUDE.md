# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

`home-library` is a Spring Boot 3.3.3 / Java 21 REST API for cataloguing a personal book collection, backed by MySQL. Books belong to a subcategory, and each subcategory belongs to a category.

## Commands

No Maven wrapper is committed (`mvnw` is gitignored), so use the system `mvn`.

```bash
mvn clean package          # build jar into target/home-library-<version>.jar
mvn spring-boot:run        # run locally (needs MySQL on localhost:3306)
mvn test                   # no test sources exist yet; src/test is absent
mvn test -Dtest=BookServiceTest#addBook   # single test / single method, once tests exist
```

Docker (from repo root):

```bash
docker compose up --build   # starts MySQL 8.0 + the app on :8080
```

The `Dockerfile` copies a **version-pinned** jar path (`target/home-library-0.2.0.jar`). Bumping `<version>` in `pom.xml` requires editing that `COPY` line too, or the image build breaks.

API surface: Swagger UI at `http://localhost:8080/swagger-ui.html` (springdoc). A Postman collection + environment (`{{url}} = http://localhost:8080`) live in [postman/](postman/), though it is stale relative to the current API (it still has `/authors` endpoints).

## Architecture

Standard layering under `com.homelibrary`, with no mapper/DTO-converter layer:

- **controller** → **service** → **repository** (Spring Data `JpaRepository`) → **model** (JPA entities).
- Request DTOs (`api/request`) are plain Lombok `@Getter` classes bound by Jackson. Response DTOs (`api/response`) are immutable and **build themselves from an entity in their constructor** (`new BookResponse(book)`), so adding a field to a response means touching the DTO constructor, not a mapper.
- Services own all lookup-and-throw logic via private `findX(id)` helpers that throw `HomeLibraryException(HttpStatus, message)`. Controllers never handle errors.
- `GlobalExceptionHandler` (`@ControllerAdvice`) converts `HomeLibraryException` (using its carried status), `MethodArgumentNotValidException` (→ 400), and any other `Exception` (→ 500) into a JSON `ErrorDetails` body. **Throw `HomeLibraryException` with the intended status rather than returning error `ResponseEntity`s from controllers.**
- `CategoryService` and `SubcategoryService` are split but both inject both repositories; `CategoryController` serves both (categories and subcategories).

### Domain relationships

`Book → Subcategory → Category` is the only real ownership chain. Convenience traversals exist in both directions and hide N+1-ish loads:

- `Book.getCategory()` delegates through `getSubcategory().getCategory()` — `Book` has **no** direct category association.
- `Category.getBooks()` is not a mapped relation; it walks every subcategory and concatenates their books. `CategoryService.deleteCategory` and `SubcategoryService.deleteSubcategory` use those collections to refuse deletion (409) when books still reference them.
- `Book.authors` is a `List<String>` on the entity (`@Column`), not an `Author` entity — an `Author` model was removed from the API earlier.
- `Priority` is an enum with an explicit `int value` (`ZERO(0)`…`HIGH(3)`) exposed over the wire as that int, mapped with plain `@Enumerated` (ordinal). Values currently coincide with ordinals — changing the enum order or inserting a constant would silently reinterpret existing rows. Convert with `Priority.fromValue(int)`, which throws `IllegalArgumentException` that services translate into a 400.

### Persistence and seed data

- Schema is managed by Hibernate: `spring.jpa.hibernate.ddl-auto=update`. Connection settings come from `MYSQL_HOST` / `MYSQL_USER` / `MYSQL_PASSWORD` env vars (defaults `localhost` / `root` / `root`), database `home_library`.
- `src/main/resources/schema.sql` and `data.sql` are **not** executed by Spring (no `spring.sql.init` config). `docker-compose.yml` only mounts them into the MySQL container at `/sql/`, so they must be sourced manually. `schema.sql` is also out of date — it still defines `author` / `author_book` tables and a `book.category_id` column that the entities no longer use. Treat `data.sql` as Polish-language sample categories, not as fixtures.

### Pagination

`GET /books` takes `pageNo` (0-based), `pageSize` (default 50), `sortParam` (default `title`), `sortDir`, and optional `priority`. The service unwraps the `Page` and returns a bare `List<BookResponse>` — no total count or page metadata reaches the client. Note `sortParam` is passed straight to `Sort.by(...)`, so an unknown property surfaces as a 500.

## Known rough edges

Present in `CategoryController` on `main`; be aware before extending it:

- The class has **no** class-level `@RequestMapping`, so `addCategory` and `getAllCategories` map to `POST /` and `GET /` while every other method is under `/categories/...` or `/subcategories/...`.
- `deleteSubcategory` declares the path as `/subcategories/{subcategoriesId}` but the parameter is `@PathVariable Integer subcategoryId` — names do not match.
- Category/subcategory create and rename endpoints take a raw `String` request body (`@RequestBody String`), not a JSON object.
- Several "no change detected" guards compare strings/boxed integers with `==` (`CategoryService.updateCategoryName`, `SubcategoryService.updateSubcategoryName`, `BookService.updateBook`), so they rarely trigger as intended.

## Style

2-space indentation, Lombok `@RequiredArgsConstructor` with `private final` dependencies for injection (no `@Autowired`), constructor-built response DTOs.
