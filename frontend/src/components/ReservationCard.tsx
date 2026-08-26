import type { ReservationItem } from '../types';

export function ReservationCard({ reservation }: { reservation: ReservationItem }) {
  const statusBadge = reservation.ready
    ? { text: reservation.statusText, className: 'badge ready' }
    : { text: reservation.statusText, className: 'badge due' };

  return (
    <div
      data-testid={`reservation-card-${reservation.id}`}
      style={{
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-md)',
        padding: '12px 14px',
        display: 'flex',
        flexDirection: 'column',
        gap: 8,
        background: 'var(--color-bg-surface)',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
        <span style={{ fontSize: 'var(--font-size-sm)', fontWeight: 'var(--font-weight-bold)', color: 'var(--color-text)', minWidth: 0, overflowWrap: 'break-word' }}>
          {reservation.title}
        </span>
        {reservation.rank != null && <span className="badge neutral" style={{ whiteSpace: 'nowrap' }}>{reservation.rank}순위</span>}
      </div>
      <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
        예약일 {reservation.reservedAt}
        {reservation.expiresAt && ` · 만기 ${reservation.expiresAt}`} · {reservation.branch} · {reservation.memberName}
      </div>
      <div>
        <span className={statusBadge.className} style={{ whiteSpace: 'nowrap' }}>{statusBadge.text}</span>
      </div>
    </div>
  );
}
