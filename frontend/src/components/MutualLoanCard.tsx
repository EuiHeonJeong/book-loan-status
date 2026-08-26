import type { MutualLoanItem } from '../types';

export function MutualLoanCard({ mutualLoan }: { mutualLoan: MutualLoanItem }) {
  const badge = { text: mutualLoan.statusText, className: mutualLoan.ready ? 'badge ready' : 'badge due' };

  return (
    <div
      data-testid={`mutual-loan-card-${mutualLoan.id}`}
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
          {mutualLoan.title}
        </span>
        <span className={badge.className} style={{ whiteSpace: 'nowrap' }}>{badge.text}</span>
      </div>
      <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
        신청일 {mutualLoan.appliedAt} · {mutualLoan.branch} → {mutualLoan.pickupBranch} · {mutualLoan.memberName}
      </div>
    </div>
  );
}
