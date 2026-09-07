import { priorityLabel } from '../constants.js';

export default function BooksTable({ books, ordinalOffset, onEdit, onDelete }) {
  return (
    <table>
      <thead>
        <tr>
          <th className="col-ordinal">Lp.</th>
          <th>Tytuł</th>
          <th>Kategoria</th>
          <th>Subkategoria</th>
          <th>Autorzy</th>
          <th>Priorytet</th>
          <th>Akcje</th>
        </tr>
      </thead>
      <tbody>
        {books.map((book, index) => (
          <tr key={book.bookId}>
            <td>{ordinalOffset + index + 1}</td>
            <td>{book.title}</td>
            <td>{book.categoryName}</td>
            <td>{book.subcategoryName}</td>
            <td>{book.authors.join(', ')}</td>
            <td>{priorityLabel(book.priority)}</td>
            <td className="actions">
              <button onClick={() => onEdit(book)}>Edytuj</button>
              <button className="danger" onClick={() => onDelete(book)}>
                Usuń
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
