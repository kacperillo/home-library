import MultiSelect from './MultiSelect.jsx';
import { SORT_DIRECTIONS, SORT_FIELDS } from '../constants.js';

export default function TopBar({
  categories,
  categoryId,
  onCategoryChange,
  subcategories,
  selectedSubcategoryIds,
  onSubcategoriesChange,
  sortParam,
  onSortParamChange,
  sortDir,
  onSortDirChange,
  onAddClick,
}) {
  return (
    <div className="bar bar-top">
      <div className="field">
        <label htmlFor="filter-category">Kategoria</label>
        <select
          id="filter-category"
          value={categoryId}
          onChange={(e) => onCategoryChange(e.target.value)}
        >
          <option value="">Wszystkie</option>
          {categories.map((category) => (
            <option key={category.categoryId} value={category.categoryId}>
              {category.categoryName}
            </option>
          ))}
        </select>
      </div>

      <div className="field">
        <label>Subkategorie</label>
        <MultiSelect
          options={subcategories.map((s) => ({
            value: s.subcategoryId,
            label: s.subcategoryName,
          }))}
          selected={selectedSubcategoryIds}
          onChange={onSubcategoriesChange}
          emptyText="Najpierw wybierz kategorię"
          disabled={categoryId === ''}
        />
      </div>

      <div className="field">
        <label htmlFor="sort-param">Sortuj po</label>
        <select
          id="sort-param"
          value={sortParam}
          onChange={(e) => onSortParamChange(e.target.value)}
        >
          {SORT_FIELDS.map((field) => (
            <option key={field.value} value={field.value}>
              {field.label}
            </option>
          ))}
        </select>
      </div>

      <div className="field">
        <label htmlFor="sort-dir">Kierunek</label>
        <select
          id="sort-dir"
          value={sortDir}
          onChange={(e) => onSortDirChange(e.target.value)}
        >
          {SORT_DIRECTIONS.map((dir) => (
            <option key={dir.value} value={dir.value}>
              {dir.label}
            </option>
          ))}
        </select>
      </div>

      <div className="spacer" />

      <button className="primary" onClick={onAddClick}>
        Dodaj
      </button>
    </div>
  );
}
