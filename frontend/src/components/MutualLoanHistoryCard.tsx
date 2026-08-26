import type { MutualLoanHistoryItem } from '../types';

export function MutualLoanHistoryCard({ item }: { item: MutualLoanHistoryItem }) {
  return (
    <div
      data-testid={`mutual-loan-history-card-${item.id}`}
      style={{
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-md)',
        padding: '12px 14px',
        display: 'flex',
        flexDirection: 'column',
        gap: 6,
        background: 'var(--color-bg-surface)',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
        <span style={{ fontSize: 'var(--font-size-sm)', fontWeight: 'var(--font-weight-bold)', color: 'var(--color-text)', minWidth: 0, overflowWrap: 'break-word' }}>
          {item.title}
        </span>
        <span className="badge neutral" style={{ whiteSpace: 'nowrap' }}>{item.statusText}</span>
      </div>
      <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
        신청일 {item.appliedAt} · {item.branch} → {item.pickupBranch} · {item.memberName}
      </div>
    </div>
  );
}
