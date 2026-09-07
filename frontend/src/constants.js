// Wartości liczbowe muszą odpowiadać enumowi Priority po stronie API:
// ZERO(0), LOW(1), MEDIUM(2), HIGH(3).
export const PRIORITIES = [
  { value: 0, label: 'ZERO' },
  { value: 1, label: 'LOW' },
  { value: 2, label: 'MEDIUM' },
  { value: 3, label: 'HIGH' },
];

export const DEFAULT_PRIORITY = 2; // MEDIUM

export const PAGE_SIZES = [10, 20, 50, 100];

export const SORT_FIELDS = [
  { value: 'title', label: 'Tytuł' },
  { value: 'priority', label: 'Priorytet' },
];

export const SORT_DIRECTIONS = [
  { value: 'asc', label: 'Rosnąco' },
  { value: 'desc', label: 'Malejąco' },
];

export function priorityLabel(value) {
  const found = PRIORITIES.find((p) => p.value === value);
  return found ? found.label : String(value);
}
