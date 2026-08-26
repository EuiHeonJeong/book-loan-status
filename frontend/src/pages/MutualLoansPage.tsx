import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AppBar } from '../components/AppBar';
import { BackIcon } from '../components/icons';
import { MutualLoanCard } from '../components/MutualLoanCard';
import { MutualLoanHistoryCard } from '../components/MutualLoanHistoryCard';
import { EmptyState } from '../components/EmptyState';
import { getMutualLoanHistory, getMutualLoans } from '../api/mutualLoans';
import type { MutualLoanHistoryResponse, MutualLoanResponse } from '../api/types';
import type { MutualLoanHistoryItem, MutualLoanItem } from '../types';

type Tab = 'current' | 'history';

function toMutualLoanItem(m: MutualLoanResponse, index: number): MutualLoanItem {
  return {
    id: `${m.title}-${m.appliedAt}-${m.memberName}-${index}`,
    title: m.title,
    branch: m.branch,
    pickupBranch: m.pickupBranch,
    appliedAt: m.appliedAt,
    statusText: m.statusText,
    ready: m.ready,
    memberName: m.memberName,
  };
}

function toMutualLoanHistoryItem(m: MutualLoanHistoryResponse, index: number): MutualLoanHistoryItem {
  return {
    id: `${m.title}-${m.appliedAt}-${m.memberName}-${index}`,
    title: m.title,
    branch: m.branch,
    pickupBranch: m.pickupBranch,
    appliedAt: m.appliedAt,
    statusText: m.statusText,
    memberName: m.memberName,
  };
}

export function MutualLoansPage() {
  const navigate = useNavigate();
  const [tab, setTab] = useState<Tab>('current');
  const [mutualLoans, setMutualLoans] = useState<MutualLoanItem[]>([]);
  const [history, setHistory] = useState<MutualLoanHistoryItem[]>([]);

  useEffect(() => {
    getMutualLoans().then((data) => setMutualLoans(data.map(toMutualLoanItem)));
    getMutualLoanHistory().then((data) => setHistory(data.map(toMutualLoanHistoryItem)));
  }, []);

  const tabs: { value: Tab; label: string }[] = [
    { value: 'current', label: '신청현황' },
    { value: 'history', label: '이력현황' },
  ];

  const items = tab === 'current' ? mutualLoans : history;

  return (
    <div className="phone">
      <AppBar
        title="상호대차현황"
        leading={
          <button className="icon-btn" onClick={() => navigate(-1)}>
            <BackIcon />
          </button>
        }
      />
      <div style={{ display: 'flex', borderBottom: '1px solid var(--color-border)' }}>
        {tabs.map((t) => (
          <div
            key={t.value}
            data-testid={`mutual-loan-tab-${t.value}`}
            onClick={() => setTab(t.value)}
            style={{
              flex: 1,
              textAlign: 'center',
              padding: '12px 0',
              fontSize: 'var(--font-size-sm)',
              fontWeight: 'var(--font-weight-bold)',
              cursor: 'pointer',
              background: tab === t.value ? 'var(--color-primary-800)' : 'var(--color-bg-surface)',
              color: tab === t.value ? '#fff' : 'var(--color-neutral-600)',
            }}
          >
            {t.label}
          </div>
        ))}
      </div>
      <div className="loan-list" style={{ flex: 1, overflowY: 'auto' }}>
        {items.length > 0 && (
          <div className="loan-list-full" style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-neutral-600)', fontWeight: 'var(--font-weight-medium)' }}>
            총 {items.length}건
          </div>
        )}
        {tab === 'current'
          ? mutualLoans.map((m) => <MutualLoanCard key={m.id} mutualLoan={m} />)
          : history.map((h) => <MutualLoanHistoryCard key={h.id} item={h} />)}
        {items.length === 0 && (
          <div className="loan-list-full">
            <EmptyState message="조회되는 도서가 없습니다" />
          </div>
        )}
      </div>
    </div>
  );
}
