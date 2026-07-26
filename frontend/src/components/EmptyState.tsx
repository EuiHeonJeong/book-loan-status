export function EmptyState({ message, icon = true }: { message: string; icon?: boolean }) {
  return (
    <div className="empty-state">
      {icon && (
        <div className="empty-state-icon" aria-hidden="true">
          🐷
        </div>
      )}
      <div>{message}</div>
    </div>
  );
}
