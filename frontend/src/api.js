const BASE = '/api';

// API zwraca błędy w dwóch formatach: własnym ({message}) dla wyjątków
// aplikacji i domyślnym Springa ({error, status}) dla reszty.
function errorMessage(body, status) {
  if (body && body.message) return body.message;
  if (body && body.error) return body.error;
  return `Błąd ${status}`;
}

async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(BASE + path, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    });
  } catch {
    throw new Error('Brak połączenia z API. Czy backend działa na porcie 8080?');
  }

  if (response.status === 204) return null;

  const text = await response.text();
  let body = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = null;
    }
  }

  if (!response.ok) throw new Error(errorMessage(body, response.status));
  return body;
}

export function getCategories() {
  return request('/categories');
}

export function getBooks({ pageNo, pageSize, sortParam, sortDir, categoryId }) {
  const params = new URLSearchParams({ pageNo, pageSize, sortParam, sortDir });
  if (categoryId !== '' && categoryId != null) params.set('categoryId', categoryId);
  return request(`/books?${params.toString()}`);
}

export function addBook(book) {
  return request('/books', { method: 'POST', body: JSON.stringify(book) });
}

export function updateBook(bookId, book) {
  return request(`/books/${bookId}`, { method: 'PUT', body: JSON.stringify(book) });
}

export function deleteBook(bookId) {
  return request(`/books/${bookId}`, { method: 'DELETE' });
}
