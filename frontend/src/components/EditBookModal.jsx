import { useMemo, useState } from 'react';
import Modal from './Modal.jsx';
import * as api from '../api.js';
import { PRIORITIES } from '../constants.js';

// Do API trafia tylko subcategoryId i priority — wybór kategorii służy
// wyłącznie do zawężenia listy subkategorii.
export default function EditBookModal({ book, categories, onClose, onSaved }) {
  const [categoryId, setCategoryId] = useState(String(book.categoryId));
  const [subcategoryId, setSubcategoryId] = useState(String(book.subcategoryId));
  const [priority, setPriority] = useState(book.priority);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const subcategories = useMemo(() => {
    const category = categories.find((c) => String(c.categoryId) === categoryId);
    return category ? category.subcategories : [];
  }, [categories, categoryId]);

  function handleCategoryChange(value) {
    setCategoryId(value);
    // Dotychczasowa subkategoria należy do innej kategorii, więc wybór trzeba
    // zacząć od nowa.
    const category = categories.find((c) => String(c.categoryId) === value);
    const first = category && category.subcategories[0];
    setSubcategoryId(first ? String(first.subcategoryId) : '');
  }

  async function submit() {
    setError(null);
    if (subcategoryId === '') {
      setError('Pole subkategorii jest puste');
      return;
    }
    setBusy(true);
    try {
      await api.updateBook(book.bookId, {
        subcategoryId: Number(subcategoryId),
        priority: Number(priority),
      });
      onSaved();
    } catch (e) {
      setError(e.message);
      setBusy(false);
    }
  }

  return (
    <Modal title={`Edycja: ${book.title}`} onClose={onClose}>
      <div className="field">
        <label htmlFor="edit-category">Kategoria</label>
        <select
          id="edit-category"
          value={categoryId}
          onChange={(e) => handleCategoryChange(e.target.value)}
        >
          {categories.map((category) => (
            <option key={category.categoryId} value={category.categoryId}>
              {category.categoryName}
            </option>
          ))}
        </select>
      </div>

      <div className="field">
        <label htmlFor="edit-subcategory">Subkategoria</label>
        <select
          id="edit-subcategory"
          value={subcategoryId}
          onChange={(e) => setSubcategoryId(e.target.value)}
        >
          <option value="">— wybierz —</option>
          {subcategories.map((subcategory) => (
            <option key={subcategory.subcategoryId} value={subcategory.subcategoryId}>
              {subcategory.subcategoryName}
            </option>
          ))}
        </select>
      </div>

      <div className="field">
        <label htmlFor="edit-priority">Priorytet</label>
        <select
          id="edit-priority"
          value={priority}
          onChange={(e) => setPriority(Number(e.target.value))}
        >
          {PRIORITIES.map((p) => (
            <option key={p.value} value={p.value}>
              {p.label}
            </option>
          ))}
        </select>
      </div>

      {error && <p className="error">{error}</p>}

      <div className="modal-actions">
        <button onClick={onClose} disabled={busy}>
          Anuluj
        </button>
        <button className="primary" onClick={submit} disabled={busy}>
          Zapisz zmiany
        </button>
      </div>
    </Modal>
  );
}
