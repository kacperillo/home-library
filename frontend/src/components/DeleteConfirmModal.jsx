import { useState } from 'react';
import Modal from './Modal.jsx';
import * as api from '../api.js';

export default function DeleteConfirmModal({ book, onClose, onDeleted }) {
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  async function confirm() {
    setBusy(true);
    setError(null);
    try {
      await api.deleteBook(book.bookId);
      onDeleted();
    } catch (e) {
      setError(e.message);
      setBusy(false);
    }
  }

  return (
    <Modal title="Usuwanie książki" onClose={onClose}>
      <p>Czy na pewno chcesz usunąć ten rekord?</p>
      <p>
        <strong>{book.title}</strong>
      </p>

      {error && <p className="error">{error}</p>}

      <div className="modal-actions">
        <button onClick={onClose} disabled={busy}>
          Nie
        </button>
        <button className="danger" onClick={confirm} disabled={busy}>
          Tak
        </button>
      </div>
    </Modal>
  );
}
