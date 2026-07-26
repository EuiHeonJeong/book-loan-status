import type { LoanRecord } from '../types';

function ddayBadge(dday: number) {
  const overdue = dday < 0;
  return {
    text: overdue ? `연체 D+${-dday}` : `D-${dday}`,
    className: overdue ? 'badge overdue' : 'badge due',
  };
}

export function LoanCard({ loan }: { loan: LoanRecord }) {
  const badge = ddayBadge(loan.dday);
  return (
    <div
      data-testid={`loan-card-${loan.id}`}
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
          {loan.title}
        </span>
        <span className={badge.className} style={{ whiteSpace: 'nowrap' }}>{badge.text}</span>
      </div>
      <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
        대출일 {loan.loanDate} · 반납예정 {loan.dueDate} · {loan.library} · {loan.memberName}
      </div>
    </div>
  );
}

export function UrgentLoanCard({ loan }: { loan: LoanRecord }) {
  const badge = ddayBadge(loan.dday);
  return (
    <div
      style={{
        minWidth: 108,
        maxWidth: 108,
        flexShrink: 0,
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-md)',
        padding: 10,
        display: 'flex',
        flexDirection: 'column',
        gap: 6,
        background: 'var(--color-primary-50)',
      }}
    >
      <div
        style={{
          fontSize: 'var(--font-size-xs)',
          fontWeight: 'var(--font-weight-medium)',
          color: 'var(--color-text)',
          display: '-webkit-box',
          WebkitLineClamp: 2,
          WebkitBoxOrient: 'vertical',
          overflow: 'hidden',
        }}
      >
        {loan.title}
      </div>
      <div
        style={{
          fontSize: 12,
          fontWeight: 700,
          color: loan.dday < 0 ? 'var(--color-accent-red)' : 'var(--color-primary-700)',
        }}
      >
        {badge.text}
      </div>
    </div>
  );
}
