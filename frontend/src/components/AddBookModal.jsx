import { useMemo, useState } from 'react';
import Modal from './Modal.jsx';
import * as api from '../api.js';
import { DEFAULT_PRIORITY, PRIORITIES } from '../constants.js';

export default function AddBookModal({ categories, onClose, onSaved }) {
  const [title, setTitle] = useState('');
  const [authors, setAuthors] = useState(['']);
  const [categoryId, setCategoryId] = useState('');
  const [subcategoryId, setSubcategoryId] = useState('');
  const [priority, setPriority] = useState(DEFAULT_PRIORITY);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const subcategories = useMemo(() => {
    const category = categories.find((c) => String(c.categoryId) === categoryId);
    return category ? category.subcategories : [];
  }, [categories, categoryId]);

  function handleCategoryChange(value) {
    setCategoryId(value);
    setSubcategoryId('');
  }

  function changeAuthor(index, value) {
    setAuthors(authors.map((a, i) => (i === index ? value : a)));
  }

  function addAuthorField() {
    // Kolejne pole ma sens dopiero wtedy, gdy poprzednie zostało wypełnione.
    if (authors[authors.length - 1].trim() === '') {
      setError('Pole autora jest puste');
      return;
    }
    setError(null);
    setAuthors([...authors, '']);
  }

  function removeAuthorField(index) {
    if (authors.length === 1) return;
    setAuthors(authors.filter((_, i) => i !== index));
  }

  async function submit() {
    if (title.trim() === '') {
      setError('Pole tytułu jest puste');
      return;
    }
    if (authors.some((a) => a.trim() === '')) {
      setError('Pole autora jest puste');
      return;
    }
    if (categoryId === '') {
      setError('Pole kategorii jest puste');
      return;
    }
    if (subcategoryId === '') {
      setError('Pole subkategorii jest puste');
      return;
    }

    setError(null);
    setBusy(true);
    try {
      await api.addBook({
        title: title.trim(),
        authors: authors.map((a) => a.trim()),
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
    <Modal title="Nowa książka" onClose={onClose}>
      <div className="field">
        <label htmlFor="add-title">Tytuł</label>
        <input
          id="add-title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
      </div>

      <div className="field">
        <label>Autorzy</label>
        {authors.map((author, index) => (
          <div className="author-row" key={index}>
            <input
              value={author}
              onChange={(e) => changeAuthor(index, e.target.value)}
              placeholder="Imię i nazwisko"
            />
            {authors.length > 1 && (
              <button type="button" onClick={() => removeAuthorField(index)}>
                −
              </button>
            )}
          </div>
        ))}
        <button type="button" onClick={addAuthorField}>
          +
        </button>
      </div>

      <div className="field">
        <label htmlFor="add-category">Kategoria</label>
        <select
          id="add-category"
          value={categoryId}
          onChange={(e) => handleCategoryChange(e.target.value)}
        >
          <option value="">— wybierz —</option>
          {categories.map((category) => (
            <option key={category.categoryId} value={category.categoryId}>
              {category.categoryName}
            </option>
          ))}
        </select>
      </div>

      <div className="field">
        <label htmlFor="add-subcategory">Subkategoria</label>
        <select
          id="add-subcategory"
          value={subcategoryId}
          onChange={(e) => setSubcategoryId(e.target.value)}
          disabled={categoryId === ''}
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
        <label htmlFor="add-priority">Priorytet</label>
        <select
          id="add-priority"
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
          Dodaj
        </button>
      </div>
    </Modal>
  );
}
