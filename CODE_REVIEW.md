# Przegląd kodu — home-library

Pierwsza wersja: 6 września 2026. Ostatnia aktualizacja: **7 września 2026**.

Dokument opisuje wykryte błędy i odstępstwa od dobrych praktyk wraz z propozycjami napraw.
Statusy w tabeli poniżej zostały zweryfikowane doświadczalnie: projekt skompilowano, aplikację
uruchomiono lokalnie na bazie MySQL wypełnionej danymi z `data.sql`, a poszczególne przypadki
sprawdzono zapytaniami HTTP i licznikiem zapytań SQL. Tam, gdzie przy punkcie widnieje wynik
testu, pochodzi on z takiego uruchomienia, a nie z samej lektury kodu.

## Jak czytać ten dokument

- **Część I** — problemy otwarte, opisane szczegółowo. To jest lista rzeczy do zrobienia.
- **Część II** — problemy rozwiązane. Zapis tego, co było nie tak i jak zostało naprawione.
- **Część III** — sugerowana kolejność dalszych prac.

Każdy otwarty problem ma stałą strukturę:

- **Gdzie** — plik i linia.
- **Na czym polega problem** — opis mechanizmu, bez zakładania wiedzy o kontekście.
- **Dlaczego to jest problem** — konkretny scenariusz: co wysyła klient i co się wtedy dzieje.
- **Proponowana zmiana** — fragment kodu pokazujący kierunek naprawy.

Numeracja punktów 1–25 pochodzi z pierwszej wersji dokumentu i celowo się nie zmienia, żeby
odwołania pozostały czytelne. Punkty 26 i dalsze to problemy wykryte później.

## Tabela statusów

| Nr | Problem | Status |
|---|---|---|
| 1 | Autorzy zapisywani jako binarna paczka | ✅ rozwiązany |
| 2 | `@RequestBody String` zamiast obiektu żądania | 🟡 częściowo |
| 3 | Aktualizacja książki — wyjątek i błędne porównania | 🟡 częściowo |
| 4 | `==` zamiast `equals` przy porównaniu nazw | ✅ rozwiązany |
| 5 | Brak `@Transactional` w serwisach | ✅ rozwiązany |
| 6 | `sortParam` i `pageSize` bez kontroli | ❌ otwarty |
| 7 | Brak metadanych stronicowania | ✅ rozwiązany |
| 8 | Obsługa wyjątków ujawnia szczegóły techniczne | ✅ rozwiązany |
| 9 | Nadmiarowe zapytania do bazy (N+1) | 🟡 częściowo |
| 10 | Brak wymuszonej unikalności nazw | ❌ otwarty |
| 11 | Wymagane pola dopuszczają NULL w bazie | ❌ otwarty |
| 12 | `@Enumerated` zapisuje pozycję stałej | ❌ otwarty |
| 13 | `ddl-auto=update` i ręczne uruchamianie plików SQL | 🟡 częściowo |
| 14 | Brak testów | ❌ otwarty |
| 15 | Klasy odpowiedzi zależne od encji | ❌ otwarty |
| 16 | Zbędny `@Setter` na serwisie | ✅ rozwiązany |
| 17 | Brak nagłówka `Location` przy 201 | ❌ otwarty |
| 18 | Zbędne ustawienie dialektu Hibernate | ✅ rozwiązany |
| 19 | Ręczne usuwanie podkategorii mimo kaskady | ✅ rozwiązany |
| 20 | Encje bez `equals` i `hashCode` | ❌ otwarty |
| 21 | Kontroler kategorii obsługiwał podkategorie | ✅ rozwiązany |
| 22 | Hasło w manifeście Kubernetesa | ❌ otwarty |
| 23 | Nieaktualna dokumentacja API | ❌ otwarty |
| 24 | Drobiazgi stylistyczne | 🟡 częściowo |
| 25 | Budowanie obrazu Dockera | ❌ otwarty (w nowej postaci) |
| 26 | Dwa różne formaty odpowiedzi błędu | ❌ nowy |
| 27 | Brak obsługi naruszeń ograniczeń bazy | ❌ nowy |
| 28 | Błędny komunikat przy usuwaniu podkategorii | ❌ nowy |
| 29 | Nieużywane importy i zależności | ❌ nowy |
| 30 | Rozjazd typów między `schema.sql` a encjami | ❌ nowy |

**Podsumowanie: 9 rozwiązanych, 6 częściowo, 15 otwartych** (w tym 5 wykrytych po pierwszej wersji).

---

# Część I: problemy otwarte

## 2. Walidacja żądań nadal niczego nie sprawdza

**Status: częściowo rozwiązany.** Główny problem zniknął — wprowadzenie klas `CategoryRequest`,
`SubcategoryRequest` i `UpdateNameRequest` sprawiło, że do bazy nie trafia już surowa treść
żądania. Została jednak druga połowa problemu.

**Gdzie:** [api/request/](src/main/java/com/homelibrary/api/request/) — wszystkie cztery klasy

**Na czym polega problem**

Adnotacja `@Valid` w kontrolerze uruchamia sprawdzanie ograniczeń zapisanych **na polach**
obiektu żądania — takich jak `@NotBlank`, `@NotNull` czy `@Size`. Żadna z klas żądań nie ma ani
jednej takiej adnotacji, więc `@Valid` przechodzi zawsze, niezależnie od zawartości.

**Dlaczego to jest problem**

Trzy konkretne scenariusze:

- `POST /categories` z ciałem `{"categoryName": ""}` utworzy kategorię o pustej nazwie.
- `POST /books` bez pola `title` skończy się naruszeniem ograniczenia `NOT NULL` w bazie, czyli
  odpowiedzią 500. Powinno to być 400 z informacją, którego pola brakuje.
- `POST /books` bez pola `authors` spowoduje `NullPointerException` w metodzie
  `findOrCreateAuthors`, bo `authorsNames.stream()` zostanie wywołane na `null`. Znowu 500.

Po usunięciu ogólnego handlera wyjątków (punkt 8) takie 500 nie zawiera już żadnego opisu, więc
klient nie dowie się nawet, co poszło nie tak.

**Proponowana zmiana**

```java
@Getter
public class CategoryRequest {
    @NotBlank(message = "Nazwa kategorii nie może być pusta")
    @Size(max = 255)
    private String categoryName;
}
```

```java
@Getter
public class BookRequest {
    @NotBlank private String title;
    @NotEmpty private List<String> authors;
    @NotNull  private Integer subcategoryId;
    @Min(0) @Max(3) private Integer priority;   // null oznacza wartość domyślną
}
```

Istniejący handler `MethodArgumentNotValidException` zamieni te naruszenia na 400 — jest już
napisany i tylko czeka, aż będzie miał co obsługiwać.

**Uwaga dodatkowa.** W serwisach ([CategoryService.java:30](src/main/java/com/homelibrary/service/CategoryService.java#L30),
[SubcategoryService.java:36](src/main/java/com/homelibrary/service/SubcategoryService.java#L36))
przy parametrach metod stoi `@Valid`. Tam ta adnotacja nie robi nic — walidacja na poziomie metod
wymaga dodatkowo `@Validated` na klasie. Warto te `@Valid` usunąć, bo sugerują zabezpieczenie,
którego nie ma. Kontroler i tak waliduje wcześniej.

---

## 3. Aktualizacja książki: wyjątek przy niekompletnym żądaniu i złamana idempotentność

**Status: częściowo rozwiązany.** Porównywanie identyfikatorów przez `==` zostało poprawione na
`equals` — to była realna usterka i zniknęła. Trzy pozostałe sprawy są otwarte.

**Gdzie:** [BookService.java:82](src/main/java/com/homelibrary/service/BookService.java#L82),
[:91](src/main/java/com/homelibrary/service/BookService.java#L91),
[BookController.java:45](src/main/java/com/homelibrary/controller/BookController.java#L45)

```java
if (book.getSubcategory().getId().equals(request.getSubcategoryId()) &&
    book.getPriority().getValue() == request.getPriority()) {
  throw new HomeLibraryException(HttpStatus.BAD_REQUEST, "No change detected");
}
```

**Sprawa A — wyjątek przy pominiętym polu `priority`.**

Wyrażenie `book.getPriority().getValue() == request.getPriority()` porównuje `int` (po lewej)
z `Integer` (po prawej). Java musi wtedy zamienić obiekt `Integer` na liczbę, czyli wywołać na
nim `intValue()`. Gdy obiekt jest `null`, wywołanie kończy się `NullPointerException`.

Konkretny przebieg: klient wysyła `PUT /books/5` z ciałem `{"subcategoryId": 3}`. Pole `priority`
jest `null` → `NullPointerException` → odpowiedź **500** bez żadnego opisu.

Metoda zmieniła się z `PATCH` na `PUT`, co oznacza „zastąp zasób w całości" i uzasadnia
wymaganie kompletu pól. Ale wymaganie pola realizuje się walidacją, a nie pozwoleniem, żeby
aplikacja wywróciła się na `null`:

```java
@Getter
public class BookUpdateRequest {
    @NotNull private Integer subcategoryId;
    @NotNull @Min(0) @Max(3) private Integer priority;
}
```

Wtedy brak pola daje czytelne 400, a warunek w serwisie może bezpiecznie porównywać wartości.

**Sprawa B — 400 przy braku zmian łamie definicję `PUT`.**

Metoda `PUT` jest z definicji **idempotentna**: wysłanie tego samego żądania dwa razy musi dwa
razy zakończyć się powodzeniem i dać ten sam stan zasobu. Obecnie drugie identyczne żądanie
zwraca 400 „No change detected". Zmiana statusu z 409 na 400 nie rozwiązała problemu, tylko
zmieniła jego numer — nadal jest to błąd zwracany w odpowiedzi na poprawne żądanie.

Klient (na przykład formularz w interfejsie) nie ma jak z góry wiedzieć, czy użytkownik cokolwiek
zmienił, i nie powinien z tego powodu dostawać błędu. Ten warunek powinien po prostu zniknąć,
a metoda zwracać 200 z aktualnym stanem książki.

**Sprawa C — `PUT` nie zastępuje całego zasobu.**

`BookUpdateRequest` zawiera wyłącznie `subcategoryId` i `priority`. Nie da się zmienić ani
tytułu, ani listy autorów. Przy `PATCH` była to niedogodność; przy `PUT`, które z definicji
zastępuje zasób w całości, jest to sprzeczność z deklarowaną metodą. Trzeba albo rozszerzyć
obiekt żądania o `title` i `authors`, albo wrócić do `PATCH` i obsłużyć `null` jako „nie
zmieniaj tego pola".

**Proponowana zmiana (wariant `PUT` z pełnym zastąpieniem)**

```java
public BookResponse updateBook(Integer bookId, BookUpdateRequest request) {
  Book book = findBook(bookId);

  book.setTitle(request.getTitle());
  book.setSubcategory(findSubcategory(request.getSubcategoryId()));
  book.setPriority(findPriority(request.getPriority()));
  updateAuthors(book, request.getAuthors());

  return new BookResponse(bookRepository.save(book));
}
```

Aktualizacja autorów wymaga uwagi na stronę właścicielską relacji — szczegóły w punkcie 20.

---

## 6. `sortParam` i `pageSize` przyjmowane bez sprawdzenia

**Status: otwarty. Potwierdzone na uruchomionej aplikacji.**

**Gdzie:** [BookService.java:58](src/main/java/com/homelibrary/service/BookService.java#L58)

```java
Sort sort = Sort.by(sortParam);
```

**Na czym polega problem**

Wartość przysłana przez klienta jest bez żadnego sprawdzenia przekazywana do Spring Data jako
nazwa pola, po którym ma nastąpić sortowanie. Gdy takie pole nie istnieje w encji `Book`,
Spring Data zgłasza `PropertyReferenceException`, którego nikt nie obsługuje.

**Dlaczego to jest problem — wynik testu**

| Zapytanie | Otrzymany status | Poprawny status |
|---|---|---|
| `GET /books?sortParam=tytul` | **500** | 400 |
| `GET /books?pageSize=100000` | 200, zwraca wszystko | 400 albo obcięcie do limitu |

Po zmianach z punktu 8 jest to **jedyny sprawdzony przypadek**, w którym zwykły błąd klienta
nadal kończy się odpowiedzią 500. W logu ląduje pełny ślad stosu, jakby doszło do awarii,
choć chodzi tylko o literówkę w nazwie parametru.

Brak górnego ograniczenia `pageSize` to osobna sprawa: `?pageSize=100000` powoduje próbę
wczytania całej tabeli do pamięci aplikacji.

**Proponowana zmiana**

```java
private static final Set<String> SORTABLE_FIELDS = Set.of("id", "title", "priority");
private static final int MAX_PAGE_SIZE = 100;

if (!SORTABLE_FIELDS.contains(sortParam)) {
  throw new HomeLibraryException(HttpStatus.BAD_REQUEST,
      "Sortowanie jest możliwe po polach: " + SORTABLE_FIELDS);
}
pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
```

Wariant bardziej zgodny z konwencjami Springa — pozwól mu zbudować `Pageable` samodzielnie.
Obsłuży wtedy standardową składnię `?page=0&size=50&sort=title,asc`, także sortowanie po kilku
polach naraz, i sam wymusi limit rozmiaru strony:

```properties
spring.data.web.pageable.max-page-size=100
spring.data.web.pageable.default-page-size=50
```

```java
@GetMapping
public ResponseEntity<BookResponsePage> getAllBooks(
    @PageableDefault(sort = "title") Pageable pageable,
    @RequestParam(required = false) Integer priority) { ... }
```

Listę dozwolonych pól warto zostawić także w tym wariancie — bez niej klient może sortować po
dowolnym polu encji.

---

## 9. Nadmiarowe zapytania do bazy przy listowaniu

**Status: częściowo rozwiązany.** Sprawdzanie „czy kategoria zawiera książki" zostało przepisane
z pętli po encjach na zapytanie `existsBySubcategoryCategoryId` — to była najkosztowniejsza część
problemu i zniknęła. Pozostałe dwie części są otwarte, a jedna się pogłębiła.

**Gdzie:** [Book.java:23](src/main/java/com/homelibrary/model/Book.java#L23),
[Book.java:26](src/main/java/com/homelibrary/model/Book.java#L26),
[CategoryResponse.java:23](src/main/java/com/homelibrary/api/response/CategoryResponse.java#L23)

**Pomiar na działającej aplikacji**

Policzyłem zapytania SQL trafiające do bazy przy pojedynczym żądaniu HTTP:

| Zapytanie HTTP | Liczba zapytań SQL |
|---|---|
| `GET /books?pageSize=10` | 18 |
| `GET /books?pageSize=50` | **69** |
| `GET /categories` (7 kategorii) | 8 |

Liczba zapytań rośnie liniowo wraz z rozmiarem strony. To klasyczny **problem N+1** — nazwa bierze
się stąd, że zamiast jednego zapytania wykonuje się jedno po listę plus po jednym dodatkowym dla
każdego z N zwróconych elementów.

**Na czym polega problem**

Dwie przyczyny nakładają się na siebie.

Po pierwsze, kolekcja autorów jest oznaczona `fetch = FetchType.EAGER`. Hibernate nie potrafi
dołączyć kolekcji do zapytania ze stronicowaniem jednym złączeniem (bo złączenie zwielokrotniłoby
wiersze i zepsuło liczenie stron), więc pobiera autorów osobnym zapytaniem **dla każdej książki**.

Po drugie, `@ManyToOne` bez parametrów oznacza `FetchType.EAGER`, więc przy każdej książce
dociągana jest podkategoria, a przy niej kategoria — nawet gdy nikt tych danych nie potrzebuje.

W `getAllCategories()` działa ten sam mechanizm: pierwsze zapytanie pobiera kategorie, a potem
`CategoryResponse` dla każdej z nich odwołuje się do `getSubcategories()`, co wyzwala osobne
zapytanie.

**Proponowana zmiana**

Ustaw wszystkie relacje jako leniwe, a tam gdzie dane są rzeczywiście potrzebne — dociągaj je
jawnie przez `@EntityGraph`:

```java
@ManyToMany(mappedBy = "books", fetch = FetchType.LAZY)
private List<Author> authors;

@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "subcategory_id", nullable = false)
private Subcategory subcategory;
```

```java
// BookRepository
@EntityGraph(attributePaths = {"authors", "subcategory", "subcategory.category"})
Page<Book> findAll(Pageable pageable);

// CategoryRepository
@EntityGraph(attributePaths = "subcategories")
List<Category> findAll();
```

Ważne: przy `LAZY` dane muszą zostać wczytane **wewnątrz transakcji**. Obecnie klasy odpowiedzi
budowane są w serwisach oznaczonych `@Transactional`, więc warunek jest spełniony — ale to
kolejny powód, żeby zająć się punktem 15.

Skalę poprawy łatwo sprawdzić: masz włączone `spring.jpa.show-sql=true`, więc wystarczy policzyć
linie `Hibernate:` w konsoli przed zmianą i po niej.

---

## 10. Brak wymuszonej unikalności nazw

**Status: otwarty. Zakres się powiększył** — doszła tabela `author`.

**Gdzie:** [Category.java:21](src/main/java/com/homelibrary/model/Category.java#L21),
[Subcategory.java:20](src/main/java/com/homelibrary/model/Subcategory.java#L20),
[Author.java:21](src/main/java/com/homelibrary/model/Author.java#L21),
[schema.sql](src/main/resources/schema.sql)

**Na czym polega problem**

Kolumny `name` mają wyłącznie ograniczenie `NOT NULL`. Nic nie stoi na przeszkodzie, żeby dodać
dziesięć kategorii o nazwie „Fizyka" albo dwa wiersze `author` o nazwie „Jan Kowalski".

**Dlaczego to jest problem**

Dla kategorii i podkategorii to problem porządkowy: dodajesz kategorię, po czasie zapominasz,
dodajesz ponownie, a książki rozdzielają się między dwa wpisy o tej samej nazwie.

Dla autorów doszedł problem techniczny, którego wcześniej nie było. Metoda
`findOrCreateAuthor` w [BookService.java:132](src/main/java/com/homelibrary/service/BookService.java#L132)
opiera się na `authorRepository.findByName(name)`, które zwraca `Optional<Author>`. Jeśli
w tabeli znajdą się dwa wiersze o tej samej nazwie, Spring Data zgłosi
`IncorrectResultSizeDataAccessException`, czyli po zmianach z punktu 8 — puste 500. Dodawanie
książek tego autora przestanie działać i nie będzie wiadomo dlaczego.

Sam mechanizm „znajdź albo utwórz" bez ograniczenia w bazie ma też okno wyścigu: dwa równoległe
żądania mogą jednocześnie nie znaleźć autora i oba go utworzyć.

Podkategorie mają dodatkową specyfikę: „Mechanika" może z sensem istnieć jednocześnie
w „Fizyce" i w „Robotyce". Unikalna powinna więc być **para** (kategoria, nazwa), a nie sama nazwa.

**Proponowana zmiana**

```java
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_category_name", columnNames = "name"))
public class Category { ... }

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
    name = "uk_subcategory_category_name", columnNames = {"category_id", "name"}))
public class Subcategory { ... }

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_author_name", columnNames = "name"))
public class Author { ... }
```

To samo trzeba dopisać w `schema.sql`, bo to on tworzy bazę (patrz punkt 13).

Ograniczenie w bazie jest jedynym pewnym zabezpieczeniem — sprawdzenie „czy taka nazwa już
istnieje" w kodzie Javy ma to samo okno wyścigu. Jego naruszenie wymaga jednak obsługi, inaczej
skończy się pustym 500 — patrz punkt 27.

**Uwaga praktyczna:** jeśli w bazie są już duplikaty, dodanie ograniczenia nie powiedzie się do
czasu ich usunięcia.

---

## 11. Wymagane pola dopuszczają NULL

**Status: otwarty, częściowo zamaskowany.**

**Gdzie:** [Book.java:26](src/main/java/com/homelibrary/model/Book.java#L26),
[Book.java:29](src/main/java/com/homelibrary/model/Book.java#L29)

**Na czym polega problem**

Adnotacja `@ManyToOne` bez parametru `optional = false` opisuje relację jako opcjonalną. To samo
dotyczy `@Enumerated` przy polu `priority`. Sprawdziłem wygenerowane mapowanie dla dialektu
MySQL — Hibernate uważa obie kolumny za dopuszczające `NULL`:

```
subcategory_id  integer   NULL
priority        tinyint   NULL
```

Sytuację częściowo ratuje `schema.sql`, w którym `subcategory_id` ma `NOT NULL`. Ponieważ bazę
utworzyłeś tym plikiem, ta akurat kolumna jest w tej chwili chroniona. Ale:

- `priority` w `schema.sql` to `tinyint DEFAULT 1`, czyli **nadal dopuszcza NULL**;
- ochrona `subcategory_id` wynika z przypadkowej zgodności, a nie z modelu — gdyby ktoś odtworzył
  schemat z encji (albo przeszedł na Flyway generowany z modelu), ograniczenie by zniknęło.

**Dlaczego to jest problem**

Cała logika aplikacji zakłada, że oba pola zawsze mają wartość:

- `Book.getCategory()` woła `getSubcategory().getCategory()` — przy braku podkategorii kończy się
  to `NullPointerException`.
- `BookResponse` woła `book.getPriority().getValue()` — przy braku priorytetu to samo.

Wystarczy jeden wiersz wstawiony z konsoli SQL bez podania priorytetu, żeby każde listowanie
książek kończyło się błędem 500.

Warto zauważyć, że wymóg obecności podkategorii jest bezpośrednią konsekwencją decyzji
o normalizacji (usunięciu `category` z `Book`): skoro kategoria wynika wyłącznie z podkategorii,
to książka bez podkategorii jest książką bez kategorii.

**Proponowana zmiana**

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "subcategory_id", nullable = false)
private Subcategory subcategory;

@Column(nullable = false)
private Priority priority;
```

oraz w `schema.sql`:

```sql
`priority` int NOT NULL DEFAULT 1,
```

---

## 12. Priorytet zapisuje się jako pozycja stałej w enumie

**Status: otwarty.**

**Gdzie:** [Book.java:29](src/main/java/com/homelibrary/model/Book.java#L29),
[Priority.java](src/main/java/com/homelibrary/model/Priority.java)

```java
@Enumerated
private Priority priority;
```

**Na czym polega problem**

Adnotacja `@Enumerated` bez podanego trybu działa jak `@Enumerated(EnumType.ORDINAL)`, czyli
zapisuje do bazy **numer porządkowy stałej** — jej pozycję w deklaracji enuma, liczoną od zera.
Nie ma to nic wspólnego z polem `value`, które sam zdefiniowałeś w klasie `Priority`.

Obecnie te dwie liczby przypadkiem się pokrywają:

| Stała | Pozycja w enumie (to trafia do bazy) | Pole `value` (to widzi klient API) |
|---|---|---|
| `ZERO` | 0 | 0 |
| `LOW` | 1 | 1 |
| `MEDIUM` | 2 | 2 |
| `HIGH` | 3 | 3 |

**Dlaczego to jest problem**

Zbieżność jest przypadkowa i nic jej nie chroni. Dopisanie nowej stałej w środku listy:

```java
public enum Priority { ZERO(0), VERY_LOW(1), LOW(2), MEDIUM(3), HIGH(4); }
```

sprawi, że wszystkie istniejące wiersze zostaną po cichu przemapowane: książki zapisane jako
`LOW` (liczba 1 w bazie) zaczną być odczytywane jako `VERY_LOW`, bo to ta stała stoi teraz na
pozycji 1. Nie pojawi się żaden błąd ani ostrzeżenie — dane po prostu zaczną znaczyć co innego.
To jeden z częściej spotykanych błędów w projektach JPA.

**Proponowana zmiana**

Skoro masz jawnie zdefiniowane `value`, zapisuj do bazy właśnie je:

```java
@Converter(autoApply = true)   // stosowany automatycznie do każdego pola typu Priority
public class PriorityConverter implements AttributeConverter<Priority, Integer> {

  @Override
  public Integer convertToDatabaseColumn(Priority priority) {
    return priority == null ? null : priority.getValue();
  }

  @Override
  public Priority convertToEntityAttribute(Integer value) {
    return value == null ? null : Priority.fromValue(value);
  }
}
```

Po dodaniu konwertera adnotację `@Enumerated` przy polu należy usunąć — te dwa mechanizmy się
wykluczają. Kolejność stałych w enumie przestaje mieć znaczenie dla danych.

**Uwaga dodatkowa.** Stała `public static final Priority defaultPriority` powinna zgodnie
z konwencją Javy nazywać się `DEFAULT_PRIORITY` — nazwy stałych pisze się wielkimi literami.
Warto też rozważyć wystawianie w API nazwy zamiast liczby (`"priority": "HIGH"`): jest to
czytelniejsze dla korzystającego z API, a Spring i Jackson zamieniają nazwy na stałe enuma
automatycznie, dzięki czemu `Priority.fromValue` i bloki `try/catch` w serwisie przestają być
potrzebne.

---

## 13. Schemat bazy powstaje automatycznie, a pliki SQL uruchamiane są ręcznie

**Status: częściowo rozwiązany.** `schema.sql` i `data.sql` zostały doprowadzone do zgodności
z modelem — `author` ma jedno pole `name`, z tabeli `book` zniknęła kolumna `category_id`,
a wiersze w `data.sql` mają poprawną liczbę wartości. Baza daje się z nich odtworzyć, co
potwierdziło uruchomienie aplikacji na wypełnionej bazie.

**Gdzie:** [application.properties](src/main/resources/application.properties),
[docker-compose.yml](docker-compose.yml)

**Co pozostało**

**Sprawa A — `ddl-auto=update` nadal zarządza schematem.** Ustawienie to sprawia, że przy każdym
starcie Hibernate porównuje encje z bazą i **dopisuje** brakujące tabele oraz kolumny, ale nigdy
niczego nie usuwa ani nie zmienia. Po kilku zmianach w modelu baza zawiera warstwy nieużywanych
kolumn, a jej kształt zależy od historii uruchomień na danej maszynie. Ryzyko jest realne:
gdybyś dodał ograniczenia z punktów 10 i 11 wyłącznie w encjach, `update` części z nich i tak by
nie zastosował do istniejących kolumn.

**Sprawa B — pliki SQL nadal nie są przez nic uruchamiane.** Spring wykonuje je tylko przy
ustawieniu `spring.sql.init.mode`, którego nie ma. `docker-compose.yml` podłącza je do kontenera
MySQL w katalogu `/sql/`, a nie do `/docker-entrypoint-initdb.d/`, który obraz MySQL uruchamia
automatycznie. Dlatego bazę trzeba było przygotować ręcznie. Najprostsza poprawka to zmiana
katalogu w `docker-compose.yml`:

```yaml
volumes:
  - mysql-data:/var/lib/mysql
  - ./src/main/resources/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql
  - ./src/main/resources/data.sql:/docker-entrypoint-initdb.d/02-data.sql
```

**Proponowany kierunek docelowy**

Narzędzie do migracji, na przykład Flyway. Każda zmiana schematu to wtedy ponumerowany plik SQL
w repozytorium, wykonywany dokładnie raz i odnotowany w tabeli historii:

```
src/main/resources/db/migration/
    V1__initial_schema.sql
    V2__unique_constraints.sql
```

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Ustawienie `validate` sprawia, że aplikacja przy starcie zgłosi błąd, jeśli encje nie pasują do
bazy — niezgodność wyjdzie od razu, a nie przy pierwszym żądaniu. Przy okazji ujawni rozjazd
typów opisany w punkcie 30.

**Uwaga.** `spring.jpa.show-sql=true` wypisuje w konsoli każde zapytanie SQL. Jest to przydatne
przy diagnozowaniu problemu z punktu 9, ale na środowisku produkcyjnym generuje ogromne ilości
logów. Docelowo warto to przenieść do profilu deweloperskiego.

---

## 14. Brak testów

**Status: otwarty.** W `pom.xml` są już wszystkie potrzebne zależności testowe
(`spring-boot-starter-webmvc-test`, `-data-jpa-test`, `-validation-test`, `-actuator-test`),
ale katalog `src/test` nie istnieje.

**Dlaczego to jest problem właśnie teraz**

Nie chodzi o zasadę „testy trzeba pisać", tylko o konkretny moment w projekcie. Po pierwsze,
większość błędów z tego dokumentu to przypadki, które prosty test wychwyciłby od razu —
`sortParam=tytul` dający 500 (punkt 6), `PUT` bez pola `priority` dający 500 (punkt 3), pusta
nazwa kategorii przechodząca walidację (punkt 2). Wszystkie znalazłem jednym przebiegiem
kilkunastu zapytań; test zrobiłby to przy każdym budowaniu.

Po drugie, planujesz wyszukiwanie z kilkoma filtrami naraz (kategoria, podkategoria, priorytet,
fraza). Liczba kombinacji do sprawdzenia rośnie wtedy szybko i ręczne przeklikiwanie ich
w Postmanie przestanie być wykonalne.

**Proponowana zmiana**

Trzy poziomy, w kolejności opłacalności:

```java
// 1. Testy repozytorium — czy zapytania faktycznie działają na bazie.
//    Najbardziej opłacalne przy wyszukiwaniu z filtrami.
@DataJpaTest
class BookRepositoryTest {
  @Test void findsBooksBySearchPhraseInAuthorName() { ... }
}
```

```java
// 2. Testy kontrolera bez bazy — kody odpowiedzi, walidacja, format JSON-a.
@WebMvcTest(BookController.class)
class BookControllerTest {
  @Test
  void returnsBadRequestWhenSortParamIsUnknown() throws Exception {
    mockMvc.perform(get("/books?sortParam=tytul")).andExpect(status().isBadRequest());
  }
}
```

```java
// 3. Testy zwykłe, bez Springa — dla logiki takiej jak Priority.fromValue.
class PriorityTest { ... }
```

Uwaga do `@DataJpaTest`: domyślnie próbuje użyć bazy w pamięci (H2), która zachowuje się inaczej
niż MySQL — inaczej traktuje ograniczenia i typy. Aby testować na tej samej bazie co produkcyjna,
użyj biblioteki Testcontainers, która uruchamia MySQL w kontenerze na czas testów.

---

## 15. Klasy odpowiedzi zależne od encji

**Status: otwarty.**

**Gdzie:** [api/response/](src/main/java/com/homelibrary/api/response/)

Klasy `BookResponse`, `CategoryResponse` i `SubcategoryResponse` przyjmują w konstruktorze encję
i same wyciągają z niej dane. Warstwa API importuje więc warstwę modelu i zna jej budowę.

Dwie konsekwencje. Po pierwsze, zmiana w encji (na przykład przemianowanie pola) wymusza zmianę
w klasie odpowiedzi, choć kontrakt API wcale nie musi się zmieniać. Po drugie — i to ważniejsze —
dane z encji odczytywane są w momencie budowania odpowiedzi. Dopóki dzieje się to wewnątrz
metody serwisu oznaczonej `@Transactional`, wszystko działa. Ale to właśnie ten układ decyduje
o tym, czy przejście na leniwe ładowanie z punktu 9 będzie bezbolesne.

Czytelniejszy układ: klasa odpowiedzi jest zwykłym `record`em bez wiedzy o encjach, a mapowaniem
zajmuje się metoda wytwórcza:

```java
public record BookResponse(
    Integer bookId, String title, List<String> authors,
    Integer categoryId, String categoryName,
    Integer subcategoryId, String subcategoryName,
    Integer priority) {

  public static BookResponse from(Book book) {
    Subcategory sub = book.getSubcategory();
    Category cat = sub.getCategory();
    return new BookResponse(book.getId(), book.getTitle(),
        book.getAuthors().stream().map(Author::getName).toList(),
        cat.getId(), cat.getName(), sub.getId(), sub.getName(), book.getPriority().getValue());
  }
}
```

`record` daje przy okazji `equals`, `hashCode` i `toString`, więc Lombok w tych klasach przestaje
być potrzebny. Ta sama uwaga dotyczy `BookResponsePage`.

---

## 17. Odpowiedzi 201 nie zawierają nagłówka `Location`

**Status: otwarty.**

**Gdzie:** metody tworzące zasoby w `BookController`, `CategoryController`, `SubcategoryController`

Zgodnie ze standardem HTTP odpowiedź 201 Created powinna zawierać nagłówek `Location` ze
wskazaniem adresu nowo utworzonego zasobu. Klient nie musi go wtedy składać samodzielnie
z identyfikatora zwróconego w ciele.

```java
URI location = ServletUriComponentsBuilder.fromCurrentRequest()
    .path("/{id}").buildAndExpand(response.getBookId()).toUri();
return ResponseEntity.created(location).body(response);
```

---

## 20. Encje bez `equals` i `hashCode`

**Status: otwarty. Waga tego punktu wyraźnie wzrosła** po wprowadzeniu relacji wiele-do-wielu.

**Gdzie:** [model/](src/main/java/com/homelibrary/model/)

**Na czym polega problem**

Bez tych metod dwa obiekty reprezentujące ten sam wiersz bazy są porównywane przez tożsamość,
czyli „czy to ten sam obiekt w pamięci".

Wcześniej był to problem teoretyczny. Teraz kod na tym polega w trzech miejscach
[BookService](src/main/java/com/homelibrary/service/BookService.java):

```java
private void removeBookFromAuthors(Book book, List<Author> authors) {
  authors.forEach(author -> author.removeBook(book));   // books.remove(book)
}

private void deleteAuthorsIfNoBooks(List<Author> authors) {
  authors.stream().filter(author -> author.getBooks().isEmpty())
      .forEach(authorRepository::delete);
}
```

`books.remove(book)` szuka obiektu w kolekcji. Dopóki wszystko dzieje się w jednej transakcji,
Hibernate gwarantuje, że dla danego wiersza istnieje dokładnie jeden obiekt, więc porównanie
przez tożsamość zadziała. Jest to jednak założenie oparte na szczególe działania Hibernate,
a nie na kodzie. Wystarczy, że obiekt książki przyjdzie spoza tej transakcji (na przykład
z innego serwisu albo z testu), żeby `remove` po cichu nic nie usunął, a `deleteAuthorsIfNoBooks`
uznał, że autor wciąż ma książki.

**Proponowana zmiana**

Ostrzeżenie: w encjach JPA **nie należy** używać lombokowych `@Data` ani `@EqualsAndHashCode` bez
ograniczenia zakresu. Domyślnie obejmują one wszystkie pola, w tym relacje, co przy leniwym
ładowaniu powoduje dociąganie całych powiązanych kolekcji przy każdym porównaniu, a przy
relacjach dwustronnych — nieskończoną rekurencję. To samo dotyczy `@ToString`.

Zalecany sposób to porównywanie po identyfikatorze:

```java
@Override
public boolean equals(Object o) {
  if (this == o) return true;
  if (!(o instanceof Book other)) return false;
  return id != null && id.equals(other.getId());
}

@Override
public int hashCode() {
  return getClass().hashCode();
}
```

Stały `hashCode` jest tu celowy: identyfikator encji zmienia się w momencie zapisu (z `null` na
nadaną wartość), a obiekt nie może zmieniać swojego `hashCode` w trakcie przebywania w kolekcji.

---

## 22. Hasło do bazy w manifeście Kubernetesa

**Status: otwarty.**

**Gdzie:** [k8s/app-deployment.yaml](k8s/app-deployment.yaml)

Hasło do bazy jest wpisane wprost w manifeście, który trafia do repozytorium. Standardowym
rozwiązaniem jest obiekt `Secret` i odwołanie przez `secretKeyRef`.

Osobno: `replicas: 10` przy jednej instancji MySQL to prawdopodobnie pozostałość po
eksperymencie. Dziesięć kopii aplikacji nie zwiększy przepustowości, jeśli wąskim gardłem jest
baza, a zwielokrotnia okna wyścigu opisane w punktach 5 i 10.

Manifesty nie zostały też zaktualizowane po zmianie `pom.xml` — odwołują się do obrazu
`kacperillo/home-library`, podczas gdy `artifactId` to teraz `homelibrary`, a wersja `1.0.0`.

---

## 23. Dokumentacja API rozjechała się z kodem

**Status: otwarty. Sytuacja się pogorszyła** po przepisaniu `pom.xml`.

**Gdzie:** [postman/home-library.postman_collection.json](postman/home-library.postman_collection.json),
[pom.xml](pom.xml)

**Na czym polega problem**

Były dwa źródła opisu API. Kolekcja Postmana jest nieaktualna: zawiera endpointy `/authors`,
wysyła kategorię i podkategorię jako nazwy tekstowe (`"category": "Physics"`), autorów jako
obiekty z polami `firstName`/`lastName`, a przy listowaniu używa parametrów
`pageNumber`/`sortDirection` zamiast `pageNo`/`sortDir`. Nie zgadza się też z nowymi klasami
żądań ani z odpowiedzią `BookResponsePage`.

Drugim źródłem był springdoc, generujący dokumentację prosto z kodu i wystawiający interfejs
Swagger pod `/swagger-ui.html`. Przy przepisywaniu `pom.xml` zależność
`springdoc-openapi-starter-webmvc-ui` zniknęła, więc te adresy przestały istnieć. Jeśli to nie
było zamierzone, warto ją przywrócić w wersji zgodnej ze Spring Boot 4 — jest to jedyna
dokumentacja, która nie może rozjechać się z kodem, bo z niego powstaje.

---

## 24. Drobiazgi stylistyczne

**Status: częściowo rozwiązany.** `getAllBooks` i `findOrCreateAuthors` używają już strumieni
zamiast ręcznego dopisywania do `ArrayList`.

Co pozostało:

- **`CategoryService.getAllCategories`** nadal buduje listę przez pustą `ArrayList` i `forEach`.
  Krócej: `categories.stream().map(CategoryResponse::new).toList()`.
- **`CategoryResponse.getSubcategories`** — to samo.
- **Komunikaty błędów są po angielsku**, a dane i dokumentacja po polsku. Warto wybrać jeden
  język i się go trzymać, zwłaszcza że komunikaty trafiają do klienta.
- **`totalElements` w `BookResponsePage` jest typu `Integer`**, a `Page.getTotalElements()`
  zwraca `long`, stąd rzutowanie `(int)` w serwisie. Przy domowej bibliotece to bez znaczenia,
  ale `long` w polu usunąłby rzutowanie i wątpliwość.
- **`BookResponsePage` nie ma pola `last` ani `hasNext`.** Klient może to policzyć z `pageNo`
  i `totalPages`, więc to kwestia wygody, nie poprawności.

---

## 25. Budowanie obrazu Dockera

**Status: otwarty w nowej postaci.** Pierwotny problem — wpisana na sztywno wersja pliku JAR —
został rozwiązany przez przejście na wzorzec `*.jar` i budowanie wieloetapowe. W nowym
`Dockerfile` jest jednak błąd, który uniemożliwia zbudowanie obrazu.

**Gdzie:** [Dockerfile](Dockerfile)

```dockerfile
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
RUN apt-get update && apt-get install -y maven && \
    mvn -f /workspace/pom.xml install -N -q && \
    mvn -f /workspace/pom.xml clean package -DskipTests -q
```

**Na czym polega problem**

Do etapu budującego kopiowany jest wyłącznie `pom.xml`. Katalog `src` nigdy nie trafia do obrazu,
więc `mvn package` nie ma czego kompilować. Budowanie przerwie się na wtyczce
`spring-boot-maven-plugin`, która nie znajdzie klasy głównej.

**Proponowana zmiana**

```dockerfile
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# Osobna warstwa na zależności — nie pobierają się ponownie przy zmianie kodu.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Użycie obrazu `maven:...` zamiast instalowania Mavena przez `apt-get` skraca budowanie i usuwa
zależność od repozytoriów pakietów systemowych. Rozdzielenie `COPY pom.xml` i `COPY src` na dwie
warstwy sprawia, że zmiana w kodzie nie wymusza ponownego pobierania wszystkich zależności.

---

## 26. Dwa różne formaty odpowiedzi błędu

**Status: nowy.** Powstał jako uboczny skutek (skądinąd słusznego) usunięcia ogólnego handlera
`Exception` — patrz punkt 8.

**Gdzie:** [ErrorDetails.java](src/main/java/com/homelibrary/exception/ErrorDetails.java),
[GlobalExceptionHandler.java](src/main/java/com/homelibrary/exception/GlobalExceptionHandler.java)

**Na czym polega problem**

Błędy wychodzą teraz z API w dwóch różnych kształtach, zależnie od tego, kto je obsłużył.
Oba poniższe pochodzą z tego samego uruchomienia i z tej samej sekundy:

```json
// obsłużone przez GlobalExceptionHandler (HomeLibraryException)
{"timeStamp":"2026-09-07T19:19:04.8290436","httpStatusCode":"400 BAD_REQUEST","message":"Invalid priority"}

// obsłużone domyślnie przez Springa (np. zły typ parametru, nieznana ścieżka)
{"timestamp":"2026-09-07T17:19:04.678Z","status":400,"error":"Bad Request","path":"/books"}
```

Różnic jest cztery i każda osobno utrudnia pracę po stronie klienta:

| | Twój format | Format Springa |
|---|---|---|
| Nazwa pola czasu | `timeStamp` | `timestamp` |
| Nazwa pola statusu | `httpStatusCode` | `status` |
| Typ statusu | tekst `"400 BAD_REQUEST"` | liczba `400` |
| Strefa czasowa | czas lokalny bez oznaczenia | UTC z sufiksem `Z` |
| Opis błędu | pole `message` | brak, jest za to `path` |

Klient nie może napisać jednej funkcji obsługującej błędy. Różnica stref jest szczególnie myląca:
te same zdarzenia wyglądają, jakby dzieliły je dwie godziny.

**Proponowana zmiana**

Dwa sensowne wyjścia.

**Wariant prostszy** — dopasuj `ErrorDetails` do kształtu Springa, żeby oba formaty różniły się
tylko obecnością pola `message`:

```java
public record ErrorDetails(Instant timestamp, int status, String error, String message) {}
```

**Wariant pełny** — niech `GlobalExceptionHandler` dziedziczy po `ResponseEntityExceptionHandler`
i nadpisuje jego metodę `handleExceptionInternal`, tak aby **wszystkie** błędy, także te
generowane przez framework, wychodziły w formacie `ErrorDetails`. Daje pełną kontrolę nad
kontraktem błędów kosztem kilkunastu linii więcej.

---

## 27. Brak obsługi naruszeń ograniczeń bazy danych

**Status: nowy.** Ujawni się dopiero po wykonaniu punktu 10, ale warto zrobić obie rzeczy razem.

**Na czym polega problem**

Po usunięciu ogólnego handlera każdy nieprzewidziany wyjątek kończy się odpowiedzią 500 bez
żadnego opisu. Dotyczy to `DataIntegrityViolationException`, czyli wyjątku zgłaszanego przy
naruszeniu ograniczenia bazy — na przykład przy próbie dodania kategorii o istniejącej nazwie,
gdy dodasz ograniczenia `UNIQUE` z punktu 10.

Klient dostanie wtedy puste 500 („awaria serwera") zamiast informacji, że taka kategoria już
istnieje.

**Proponowana zmiana**

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ErrorDetails> handle(DataIntegrityViolationException e) {
  log.warn("Naruszenie ograniczenia bazy danych", e);
  return ResponseEntity.status(HttpStatus.CONFLICT)
      .body(new ErrorDetails(LocalDateTime.now(), HttpStatus.CONFLICT,
          "Element o takiej nazwie już istnieje"));
}
```

Zwróć uwagę na sposób logowania: wyjątek przekazany jako **ostatni argument bez odpowiadającego
mu `{}`** trafia do logu razem z pełnym śladem stosu. Zapis `log.warn("...: {}", e.getMessage())`
zapisałby sam tekst, bez informacji, gdzie błąd powstał. Ta sama uwaga dotyczy dwóch istniejących
metod w `GlobalExceptionHandler`, które nadal logują przez `e.getMessage()`.

---

## 28. Błędny komunikat przy usuwaniu podkategorii

**Status: nowy.**

**Gdzie:** [SubcategoryService.java:48](src/main/java/com/homelibrary/service/SubcategoryService.java#L48)

```java
if (!subcategoryRepository.existsById(subcategoryId)) {
  throw new HomeLibraryException(HttpStatus.NOT_FOUND, "Category with given ID does not exist");
}
```

Sprawdzana jest podkategoria, a komunikat mówi o kategorii. Klient usuwający podkategorię
dostanie informację, że nie istnieje kategoria — i może zacząć szukać problemu w zupełnie innym
miejscu. Poprawny tekst to „Subcategory with given ID does not exist".

---

## 29. Nieużywane importy i zależności

**Status: nowy.** Drobiazg porządkowy, ale wprowadza w błąd przy czytaniu kodu.

- [AuthorRepository.java](src/main/java/com/homelibrary/repository/AuthorRepository.java) —
  importuje `Priority`, `Page` i `Pageable`, z których żaden nie jest używany.
- [SubcategoryService.java](src/main/java/com/homelibrary/service/SubcategoryService.java) —
  został import `lombok.Setter` po usuniętej adnotacji.
- [CategoryService.java](src/main/java/com/homelibrary/service/CategoryService.java) —
  wstrzykuje `subcategoryRepository`, które po przepisaniu `deleteCategory` nie jest już nigdzie
  używane.

---

## 30. Rozjazd typów między `schema.sql` a encjami

**Status: nowy.** Nie przeszkadza dzisiaj, ale zablokuje przejście na `ddl-auto=validate`
z punktu 13.

**Gdzie:** [schema.sql](src/main/resources/schema.sql), [model/](src/main/java/com/homelibrary/model/)

**Na czym polega problem**

Identyfikatory w encjach są typu `Integer`, który Hibernate mapuje na kolumnę `int`. W `schema.sql`
wszystkie identyfikatory i klucze obce zadeklarowano jako `bigint`. Dopóki schematem zarządza
`ddl-auto=update`, a bazę tworzysz plikiem, nikt tych typów nie porównuje. Po przejściu na
`validate` aplikacja odmówi startu z komunikatem o niezgodności typów.

Do rozstrzygnięcia jest, która strona ma ustąpić:

- **zmiana `schema.sql` na `int`** — mniej pracy, zero zmian w kodzie;
- **zmiana identyfikatorów w encjach na `Long`** — w projektach JPA konwencja częstsza, daje
  zapas na przyszłość, ale wymaga przejścia po wszystkich sygnaturach (`Integer bookId`,
  `JpaRepository<Book, Integer>`, typy w klasach odpowiedzi).

Ważne, żeby obie strony mówiły to samo.

**Przy okazji, w tym samym pliku:**

- tabela `author_book` nie ma klucza głównego ani ograniczenia unikalności, więc ten sam autor
  może zostać przypisany do tej samej książki wielokrotnie. Powinno być
  `PRIMARY KEY (author_id, book_id)`;
- brak jawnego `ENGINE` i kodowania przy tabelach — plik polega na domyślnych ustawieniach
  serwera. Przy polskich znakach w tytułach i nazwiskach warto podać
  `ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci` jawnie;
- klucze obce nie mają nazw, więc komunikat o naruszeniu ograniczenia zawiera wygenerowaną nazwę
  w rodzaju `book_ibfk_1`, z której nic nie wynika.

---

# Część II: problemy rozwiązane

Zapis tego, co było nie tak i jak zostało naprawione. Zachowany, żeby nie wracać do tych samych
rozwiązań i żeby było wiadomo, dlaczego kod wygląda tak, jak wygląda.

## Naprawa środowiska budowania (nie było w pierwszej wersji)

**Problem.** Projekt nie kompilował się w ogóle: JDK 25 od wersji 23 domyślnie nie uruchamia
procesorów adnotacji znalezionych na classpath, więc Lombok był po cichu pomijany i wszystkie
generowane gettery znikały (102 błędy „cannot find symbol"). Dodatkowo Lombok w wersji narzuconej
przez Spring Boot 3.3.3 nie obsługiwał JDK 25.

**Naprawa.** Jawne wskazanie procesorów przez `annotationProcessorPaths` w konfiguracji
`maven-compiler-plugin` oraz przejście na Spring Boot 4.1.1. Rozwiązanie lepsze niż samo
włączenie `-proc:full` — działa niezależnie od domyślnych ustawień kolejnych wersji javaca.

## 1. Autorzy zapisywani jako binarna paczka

**Problem.** Pole `List<String> authors` z samą adnotacją `@Column` nie było kolekcją w rozumieniu
JPA. Hibernate mapował je na jedną kolumnę `authors varbinary(255)` z zserializowanym obiektem
Javy — nie dało się tego przeszukać, zaindeksować ani odczytać w SQL-u, a limit 255 bajtów groził
błędem zapisu. Blokowało to planowane wyszukiwanie po autorze.

**Naprawa.** Osobna encja `Author` z relacją `@ManyToMany` i tabelą łączącą `author_book`, wraz
z logiką „znajdź albo utwórz autora" w `BookService` i usuwaniem autorów bez książek. Uwaga na
stronę właścicielską: `@JoinTable` jest na `Author`, `Book` ma `mappedBy`, więc zapis relacji
działa wyłącznie przez `author.addBook(book)` — i tak właśnie jest to zrobione.

**Weryfikacja.** `GET /books` zwraca autorów jako nazwy:
`{"authors":["Julian Klukowski","Ireneusz Nabiałek"],"title":"Algebra dla studentów", ...}`.

## 4. `==` zamiast `equals` przy porównaniu nazw

**Problem.** `category.getName() == newCategoryName` porównywało tożsamość obiektów, a nie treść
napisów, więc warunek nigdy się nie uruchamiał — zabezpieczenie wyglądało na działające, a nie
działało.

**Naprawa.** Zamiana na `equals`. Pozostaje otwarta kwestia, czy zmiana nazwy na tę samą powinna
w ogóle kończyć się błędem 409 — to ta sama wątpliwość co w punkcie 3.

## 5. Brak `@Transactional` w serwisach

**Problem.** Każde wywołanie repozytorium tworzyło osobną transakcję. `deleteCategory` sprawdzało
„czy kategoria zawiera książki" i usuwało ją w dwóch różnych transakcjach, więc między
sprawdzeniem a usunięciem mogła zostać dodana książka. Dostęp do leniwych kolekcji działał
wyłącznie dzięki domyślnie włączonemu `spring.jpa.open-in-view`.

**Naprawa.** `@Transactional` na wszystkich trzech serwisach plus przepisanie `deleteCategory`
i `deleteSubcategory` na `existsById` + `existsBy...` + `deleteById`, dzięki czemu sprawdzenie
i usunięcie dzieją się w jednej transakcji. Przy okazji doszła kontrola istnienia zasobu, więc
usunięcie nieistniejącego identyfikatora zwraca 404 zamiast przechodzić bez śladu.

**Do rozważenia.** Użyto `jakarta.transaction.Transactional`. Wersja Springa
(`org.springframework.transaction.annotation.Transactional`) daje dodatkowo `readOnly = true` dla
metod odczytujących i kontrolę nad propagacją. Nadal nie ustawiono `spring.jpa.open-in-view=false`.

## 7. Brak metadanych stronicowania

**Problem.** `Page` był rozpakowywany do gołej listy, więc klient nie znał liczby wszystkich
wyników ani liczby stron i nie mógł zbudować nawigacji.

**Naprawa.** Klasa `BookResponsePage` z polami `content`, `pageNo`, `pageSize`, `totalElements`,
`totalPages`, zwracana przez `getAllBooks` i kontroler.

**Weryfikacja.** `GET /books?pageSize=2` → `totalElements: 118, totalPages: 59`;
`GET /books?priority=1&pageSize=2` → `totalElements: 43, totalPages: 22`; strona poza zakresem
zwraca pustą listę z zachowanymi metadanymi.

## 8. Obsługa wyjątków ujawniała szczegóły techniczne

**Problem.** Handler `@ExceptionHandler(Exception.class)` przechwytywał wszystko, w tym wyjątki,
które Spring sam tłumaczy na właściwe kody HTTP. Efekt: nieistniejąca ścieżka i zły typ parametru
kończyły się odpowiedzią 500. Treść wyjątku trafiała przy tym do klienta (`"No property 'tytul'
found for type 'Book'"` ujawniało nazwę encji), a log zawierał sam komunikat, bez śladu stosu.

**Naprawa.** Usunięcie ogólnego handlera `Exception`.

**Weryfikacja.** Wszystkie trzy części problemu zniknęły:

| Zapytanie | Przed | Po |
|---|---|---|
| `GET /nie-ma-takiej-sciezki` | 500 | **404** |
| `GET /books?priority=abc` | 500 | **400** |
| treść odpowiedzi 500 | nazwa encji w `message` | ogólny komunikat Springa |
| log przy nieobsłużonym błędzie | sam tekst wyjątku | pełny ślad stosu |

Skutki uboczne opisano w punktach 26 i 27.

## 16. Zbędny `@Setter` na klasie serwisu

**Problem.** Adnotacja generowała metody ustawiające zależności w bezstanowym komponencie.

**Naprawa.** Usunięta. Został nieużywany import — patrz punkt 29.

## 18. Zbędne ustawienie dialektu Hibernate

**Problem.** `spring.jpa.properties.hibernate.dialect` powodowało ostrzeżenie `HHH90000025`;
Hibernate 6 sam rozpoznaje rodzaj bazy, a jawne wskazanie ogólnego dialektu może wyłączyć
optymalizacje dostępne dla konkretnej wersji serwera.

**Naprawa.** Linia usunięta z `application.properties`.

## 19. Ręczne usuwanie podkategorii mimo skonfigurowanej kaskady

**Problem.** `deleteCategory` przechodziło pętlą po podkategoriach i usuwało je pojedynczo, mimo
że relacja ma `cascade = CascadeType.REMOVE`.

**Naprawa.** Pętla usunięta, zostało `categoryRepository.deleteById(categoryId)`.

## 21. Kontroler kategorii obsługiwał także podkategorie

**Problem.** Jedna klasa obsługiwała dwa niezależne zestawy adresów, a dwa endpointy przez brak
`@RequestMapping` na klasie trafiały pod ścieżkę główną `/`.

**Naprawa.** `@RequestMapping("/categories")` na `CategoryController` i wydzielenie
`SubcategoryController` z `@RequestMapping("/subcategories")`.

---

# Część III: sugerowana kolejność dalszych prac

Kolejność wynika z zależności między punktami — wcześniejsze ułatwiają późniejsze.

**Krok 1 — dokończyć to, co już zaczęte (małe, szybkie zmiany).**
Punkt 6 (biała lista pól sortowania i limit `pageSize`) — to jedyny sprawdzony przypadek, w którym
zwykły błąd klienta nadal daje 500. Punkt 2 (adnotacje walidacyjne w klasach żądań) — handler
`MethodArgumentNotValidException` jest już napisany i tylko czeka. Punkt 3 (`@NotNull` na
`BookUpdateRequest` i usunięcie warunku „No change detected"). Punkty 28 i 29 przy okazji.

**Krok 2 — spójność błędów.**
Punkty 26 i 27 razem: ujednolicony format `ErrorDetails` plus handler naruszeń ograniczeń bazy.
Punkt 27 jest warunkiem wstępnym dla kroku 3.

**Krok 3 — zacieśnienie modelu (zmiany w schemacie bazy).**
Punkty 10, 11, 12, 30 wykonane jednym podejściem, bo wszystkie dotykają `schema.sql` i encji.
Warto przy tej okazji rozstrzygnąć `Integer` kontra `Long`.

**Krok 4 — pierwsze testy.**
Punkt 14, zanim dojdą filtry i wyszukiwanie. Wtedy przyniosą największą korzyść, a przypadki
z kroku 1 stanowią gotową listę pierwszych testów.

**Krok 5 — wydajność.**
Punkt 9 (leniwe relacje i `@EntityGraph`), najlepiej po punkcie 15, bo to on decyduje o tym,
gdzie dane są odczytywane. Punkt 13 (Flyway i `ddl-auto=validate`) domyka temat schematu.

**Krok 6 — reszta.**
Punkty 15, 17, 20, 22, 23, 24, 25 — do zrobienia przy okazji, w dowolnej kolejności.

Punkt 25 (`Dockerfile`) warto wykonać wcześniej, jeśli planujesz uruchamiać aplikację
w kontenerze — w obecnej postaci obraz się nie zbuduje.
