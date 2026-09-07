import { useEffect, useRef, useState } from 'react';

// Lista rozwijana z możliwością zaznaczenia wielu pozycji naraz.
// Natywny <select multiple> jest niewygodny w obsłudze, więc to przycisk
// otwierający panel z polami wyboru.
export default function MultiSelect({ options, selected, onChange, emptyText, disabled }) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef(null);

  // Zamknięcie panelu po kliknięciu poza nim.
  useEffect(() => {
    if (!open) return;
    function handleClickOutside(event) {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [open]);

  function toggle(value) {
    if (selected.includes(value)) {
      onChange(selected.filter((v) => v !== value));
    } else {
      onChange([...selected, value]);
    }
  }

  const summary =
    selected.length === 0 ? 'Wszystkie' : `Wybrano: ${selected.length}`;

  return (
    <div className="multiselect" ref={containerRef}>
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        disabled={disabled}
      >
        {summary} ▾
      </button>

      {open && (
        <div className="multiselect-panel">
          {options.length === 0 && (
            <div className="multiselect-empty">{emptyText}</div>
          )}
          {options.map((option) => (
            <label key={option.value}>
              <input
                type="checkbox"
                checked={selected.includes(option.value)}
                onChange={() => toggle(option.value)}
              />{' '}
              {option.label}
            </label>
          ))}
        </div>
      )}
    </div>
  );
}
