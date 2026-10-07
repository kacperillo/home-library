# Home Library — desktop (wersja prosta)

Minimalna aplikacja okienkowa (Java 25, Swing) dla API `home-library`: tabela książek
z filtrem po kategorii i subkategorii, sortowaniem, stronicowaniem oraz dodawaniem,
edycją i usuwaniem. Jedyna zależność poza JDK to Jackson (JSON).

W odróżnieniu od `desktop/` nie odwzorowuje frontendu React 1:1 — korzysta z tego,
co Swing daje za darmo, kosztem kilku uproszczeń w interfejsie.

## Uruchomienie

```bash
# backend w katalogu głównym repozytorium (potrzebny MySQL na 3306)
mvn spring-boot:run

# w katalogu desktop-simple/
mvn package
java -jar target/home-library-desktop-simple.jar                    # http://localhost:8080
java -jar target/home-library-desktop-simple.jar http://host:8080   # inny adres API
```

## Pliki

```
src/main/java/com/homelibrary/simple/
  Main.java            punkt wejścia
  Api.java             klient HTTP + rekordy odpowiedzi (Book, Page, Category, Subcategory, Priority)
  Item.java            pozycja combo: id + etykieta
  BookTableModel.java  model tabeli
  BookForm.java        formularz dodawania/edycji pokazywany przez JOptionPane
  MultiSelect.java     przycisk rozwijający panel z checkboxami (wybór subkategorii)
  MainFrame.java       okno główne: filtry, tabela, stronicowanie, akcje
```

## Jak to jest zrobione (i dlaczego prościej niż w `desktop/`)

- **Wielokrotny wybór subkategorii.** Swing nie ma combo z checkboxami, więc `MultiSelect`
  to jedyny własny komponent: `JButton`, który otwiera `JPopupMenu` z `JCheckBox`ami
  (~40 linii). Panel nie zamyka się po kliknięciu w checkbox, bo checkbox nie jest
  `JMenuItem`. Brak zaznaczenia oznacza „wszystkie”.
- **Stan trzymają widgety.** `MainFrame.reload()` czyta bieżące wartości z combo i `MultiSelect`
  (`getSelectedItem()`, `getSelectedIds()`) zamiast trzymać ich kopie w polach klasy. Nie ma interfejsów
  słuchaczy między komponentami — każdy combo ma jedną linijkę `addActionListener(e -> reload(0))`.
- **Akcje na zaznaczonym wierszu.** Przyciski *Edytuj* / *Usuń* stoją nad tabelą i działają
  na zaznaczonym rekordzie (dwuklik = edycja). Znika cała warstwa renderer/editor dla
  przycisków w komórkach `JTable`.
- **Dialogi przez `JOptionPane`.** Usuwanie to `showConfirmDialog` z pytaniem; formularz
  dodawania/edycji to `JPanel` (`BookForm`) przekazany do `showConfirmDialog` jako treść.
  Walidacja w pętli: przy błędzie pokazuje komunikat i wraca do formularza z zachowanymi wartościami.
- **Błędy API** lądują w `JOptionPane.showMessageDialog`, a nie w osobnym pasku błędu.
- **Layout:** `FlowLayout` dla pasków, `BorderLayout` dla okna, `GridLayout(0, 2)` dla formularza.
  Żadnych ręcznych rozmiarów poza `setPrototypeDisplayValue` dla combo.
- **Wątki:** jedna metoda `run(Callable, Consumer)` opakowuje `SwingWorker`; na czas żądania
  kursor zmienia się w klepsydrę.

## Uproszczenia względem frontendu React

- Sortowanie: jedno combo z czterema opcjami (pole + kierunek).
- Autorzy: jedno pole tekstowe, nazwiska oddzielone przecinkami.
- Stronicowanie: przyciski ‹ › i etykieta; bez pola do wpisania numeru strony.
- Bez kolumny „Lp.” i bez ekranu „Wczytywanie…”.

## Priorytety

`Api.Priority` to enum `ZERO, LOW, MEDIUM, HIGH`, którego `ordinal()` jest wartością
wysyłaną do API — kolejność musi zgadzać się z enumem `Priority` w backendzie.
