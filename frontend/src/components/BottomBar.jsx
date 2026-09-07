import { useEffect, useState } from 'react';
import { PAGE_SIZES } from '../constants.js';

// pageNo z API jest liczone od zera, użytkownikowi pokazujemy numery od jedynki.
export default function BottomBar({
  pageNo,
  totalPages,
  totalElements,
  pageSize,
  onPageChange,
  onPageSizeChange,
}) {
  const [inputValue, setInputValue] = useState(String(pageNo + 1));

  // Po zmianie strony strzałkami pole musi pokazać nową wartość.
  useEffect(() => {
    setInputValue(String(pageNo + 1));
  }, [pageNo]);

  function commitInput() {
    const parsed = Number.parseInt(inputValue, 10);
    if (Number.isNaN(parsed)) {
      setInputValue(String(pageNo + 1));
      return;
    }
    const lastPage = Math.max(totalPages, 1);
    const clamped = Math.min(Math.max(parsed, 1), lastPage);
    setInputValue(String(clamped));
    onPageChange(clamped - 1);
  }

  const isFirst = pageNo <= 0;
  const isLast = totalPages === 0 || pageNo >= totalPages - 1;

  return (
    <div className="bar bar-bottom">
      <button onClick={() => onPageChange(pageNo - 1)} disabled={isFirst}>
        ‹ Poprzednia
      </button>

      <span>Strona</span>
      <input
        style={{ width: 56 }}
        value={inputValue}
        onChange={(e) => setInputValue(e.target.value)}
        onBlur={commitInput}
        onKeyDown={(e) => {
          if (e.key === 'Enter') e.target.blur();
        }}
      />
      <span>z {totalPages}</span>

      <button onClick={() => onPageChange(pageNo + 1)} disabled={isLast}>
        Następna ›
      </button>

      <div className="field">
        <label htmlFor="page-size">Na stronie</label>
        <select
          id="page-size"
          value={pageSize}
          onChange={(e) => onPageSizeChange(Number(e.target.value))}
        >
          {PAGE_SIZES.map((size) => (
            <option key={size} value={size}>
              {size}
            </option>
          ))}
        </select>
      </div>

      <div className="spacer" />
      <span>Wszystkich książek: {totalElements}</span>
    </div>
  );
}
