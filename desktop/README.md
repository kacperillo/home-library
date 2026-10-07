# Home Library — desktop

Aplikacja okienkowa (Java 25, Swing) dla API `home-library`. Pokazuje tę samą tabelę książek
co frontend w React: filtry po kategorii i subkategoriach, sortowanie, stronicowanie oraz
dodawanie, edycję i usuwanie rekordów. Jedyna zależność poza JDK to Jackson (JSON).

## Uruchomienie

Potrzebny jest działający backend (domyślnie `http://localhost:8080`).

```bash
# 1. w katalogu głównym repozytorium — API na porcie 8080
mvn spring-boot:run

# 2. w katalogu desktop/ — budowa wykonywalnego jara
mvn package
java -jar target/home-library-desktop.jar
```

Adres API można nadpisać (w kolejności pierwszeństwa):

```bash
java -jar target/home-library-desktop.jar http://inny-host:8080
java -Dhomelibrary.url=http://inny-host:8080 -jar target/home-library-desktop.jar
HOME_LIBRARY_URL=http://inny-host:8080 java -jar target/home-library-desktop.jar
```

Aplikacja łączy się z API bezpośrednio, więc — inaczej niż frontend w przeglądarce —
nie potrzebuje ani proxy, ani konfiguracji CORS.

## Układ plików

Struktura celowo odpowiada frontendowi w React, żeby zmiany w jednym kliencie łatwo
było przenieść do drugiego.

```
src/main/java/com/homelibrary/desktop/
  Main.java                  punkt wejścia, wybór adresu API, look & feel
  Constants.java             priorytety, rozmiary strony, opcje sortowania   (constants.js)
  Option.java                pozycja listy rozwijanej: wartość + etykieta
  api/
    ApiClient.java           wywołania HTTP i ujednolicenie komunikatów błędów (api.js)
    ApiException.java        błąd z komunikatem gotowym do pokazania użytkownikowi
    Book, BookPage, Category, Subcategory   odpowiedzi API (rekordy)
    NewBook, BookUpdate      treść żądań POST /books i PUT /books/{id}
  ui/
    MainFrame.java           stan widoku, pobieranie danych, składanie całości (App.jsx)
    TopBar.java              filtry, sortowanie, przycisk „Dodaj”            (TopBar.jsx)
    MultiSelect.java         lista rozwijana z wielokrotnym wyborem          (MultiSelect.jsx)
    BooksTable.java          tabela z rekordami i przyciskami akcji          (BooksTable.jsx)
    BottomBar.java           stronicowanie i liczba pozycji na stronie       (BottomBar.jsx)
    FormDialog.java          wspólna ramka okna modalnego                    (Modal.jsx)
    AddBookDialog.java       formularz nowej książki                         (AddBookModal.jsx)
    EditBookDialog.java      zmiana subkategorii i priorytetu                (EditBookModal.jsx)
    DeleteConfirmDialog.java potwierdzenie usunięcia                         (DeleteConfirmModal.jsx)
    Async.java               uruchamianie wywołań API poza wątkiem GUI
    Ui.java                  drobne pomocniki układu
```

## Jak to działa

- Każde wywołanie API idzie przez `SwingWorker` (`Async.run`), żeby nie blokować wątku
  zdarzeń; wynik i błąd wracają na EDT. Na czas żądania przyciski dialogu są nieaktywne.
- Oba filtry są przetwarzane po stronie API (`GET /books?categoryId=&subcategoryIds=1&subcategoryIds=2`),
  więc stronicowanie i licznik u dołu odnoszą się zawsze do przefiltrowanego wyniku.
  Zmiana filtru, sortowania lub rozmiaru strony resetuje numer strony.
- Po usunięciu ostatniego rekordu na ostatniej stronie numer strony wykracza poza zakres —
  `MainFrame.loadBooks` cofa się wtedy na ostatnią istniejącą stronę i pobiera dane ponownie.
- Kolumna „Akcje” w `JTable` jest jedyną „edytowalną”: jej edytor komórki to panel z przyciskami,
  dzięki czemu przyciski reagują na pojedyncze kliknięcie.
- Błędy API przychodzą w dwóch formatach: własnym (`{message}`) i domyślnym Springa
  (`{error, status}`). `ApiClient` sprowadza oba do jednego komunikatu, a brak połączenia
  do osobnej podpowiedzi o adresie backendu.

## Priorytety

Wartości liczbowe w `Constants.java` muszą odpowiadać enumowi `Priority` w API:
`ZERO(0)`, `LOW(1)`, `MEDIUM(2)`, `HIGH(3)`. Formularz dodawania domyślnie ustawia `MEDIUM`.
