// Wspólna ramka okna modalnego: przyciemnione tło i biały prostokąt.
export default function Modal({ title, children, onClose }) {
  return (
    <div
      className="modal-backdrop"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="modal">
        <h2>{title}</h2>
        {children}
      </div>
    </div>
  );
}
