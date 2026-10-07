# Home Library — frontend

Widok tabeli z książkami dla API `home-library`. React + Vite, bez dodatkowych bibliotek.

## Uruchomienie

Potrzebne są dwa procesy: backend i serwer deweloperski frontendu.

```bash
# 1. w katalogu głównego repozytorium — API na porcie 8080
mvn spring-boot:run

# 2. w katalogu frontend/ — interfejs na porcie 5173
npm install     # tylko za pierwszym razem
npm run dev
```

Następnie otwórz `http://localhost:5173`.

## Dlaczego działa bez konfiguracji CORS

Przeglądarka blokowałaby żądania z portu 5173 do 8080 jako żądania do innego źródła.
Zamiast konfigurować CORS po stronie Springa, serwer deweloperski Vite przekazuje wszystko,
co zaczyna się od `/api`, do `http://localhost:8080` (patrz `vite.config.js`). Dla przeglądarki
frontend i API są więc tym samym źródłem, a backend pozostaje nietknięty.

Przy budowaniu wersji produkcyjnej (`npm run build`) tego pośrednika nie ma — trzeba wtedy albo
włączyć CORS w API, albo serwować pliki z `dist/` spod tego samego adresu co API.

## Układ plików

```
src/
  App.jsx                  stan widoku, pobieranie danych, składanie całości
  api.js                   wywołania HTTP i ujednolicenie komunikatów błędów
  constants.js             priorytety, rozmiary strony, opcje sortowania
  styles.css
  components/
    TopBar.jsx             filtry, sortowanie, przycisk „Dodaj”
    MultiSelect.jsx        lista rozwijana z wielokrotnym wyborem (subkategorie)
    BooksTable.jsx         tabela z rekordami
    BottomBar.jsx          stronicowanie i liczba pozycji na stronie
    Modal.jsx              wspólna ramka okna modalnego
    AddBookModal.jsx       formularz nowej książki
    EditBookModal.jsx      zmiana subkategorii i priorytetu
    DeleteConfirmModal.jsx potwierdzenie usunięcia
```

## Filtry

Oba filtry są przetwarzane po stronie API, więc stronicowanie i licznik pozycji u dołu
odnoszą się zawsze do przefiltrowanego wyniku.

- **Kategoria** — jedna wartość, `GET /books?categoryId=`.
- **Subkategorie** — wiele wartości naraz, `GET /books?categoryId=&subcategoryIds=1&subcategoryIds=2`
  (API przyjmuje też formę `subcategoryIds=1,2`). Brak zaznaczenia oznacza „wszystkie”.
  Lista jest pusta, dopóki nie wybrano kategorii, dzięki czemu wysyłane są tylko subkategorie
  należące do wybranej kategorii — API i tak to weryfikuje i odrzuca niezgodne (400) lub
  nieistniejące (404) identyfikatory.

Zmiana dowolnego filtru wywołuje nowe żądanie i resetuje numer strony.

## Priorytety

Wartości liczbowe w `constants.js` muszą odpowiadać enumowi `Priority` w API:
`ZERO(0)`, `LOW(1)`, `MEDIUM(2)`, `HIGH(3)`. Formularz dodawania domyślnie ustawia `MEDIUM`.
