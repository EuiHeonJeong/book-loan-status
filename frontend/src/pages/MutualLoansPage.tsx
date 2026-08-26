import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AppBar } from '../components/AppBar';
import { BackIcon } from '../components/icons';
import { MutualLoanCard } from '../components/MutualLoanCard';
import { MutualLoanHistoryCard } from '../components/MutualLoanHistoryCard';
import { EmptyState } from '../components/EmptyState';
import { getMutualLoanHistory, getMutualLoans } from '../api/mutualLoans';
import { listMembers } from '../api/members';
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
  const [filterOpen, setFilterOpen] = useState(false);
  const [tab, setTab] = useState<Tab>('current');
  const [familyNames, setFamilyNames] = useState<string[]>([]);
  const [family, setFamily] = useState<Record<string, boolean>>({});
  const [mutualLoans, setMutualLoans] = useState<MutualLoanItem[]>([]);
  const [history, setHistory] = useState<MutualLoanHistoryItem[]>([]);

  useEffect(() => {
    listMembers().then((members) => {
      const names = members.map((m) => m.name);
      setFamilyNames(names);
      setFamily(Object.fromEntries(names.map((n) => [n, true])));
    });
    getMutualLoans().then((data) => setMutualLoans(data.map(toMutualLoanItem)));
    getMutualLoanHistory().then((data) => setHistory(data.map(toMutualLoanHistoryItem)));
  }, []);

  const toggleFamily = (name: string) => setFamily((f) => ({ ...f, [name]: !f[name] }));

  const filteredMutualLoans = useMemo(
    () => mutualLoans.filter((m) => family[m.memberName] !== false),
    [mutualLoans, family]
  );
  const filteredHistory = useMemo(() => history.filter((h) => family[h.memberName] !== false), [history, family]);

  const tabs: { value: Tab; label: string }[] = [
    { value: 'current', label: '신청현황' },
    { value: 'history', label: '이력현황' },
  ];

  const items = tab === 'current' ? filteredMutualLoans : filteredHistory;

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
      <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column' }}>
        <div style={{ borderBottom: '1px solid var(--color-border)' }}>
          <div
            style={{ padding: '12px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer' }}
            onClick={() => setFilterOpen((v) => !v)}
          >
            <span style={{ fontSize: 'var(--font-size-sm)', fontWeight: 'var(--font-weight-bold)', color: 'var(--color-text)' }}>필터</span>
            <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-neutral-600)' }}>{filterOpen ? '▾ 접기' : '▸ 펼치기'}</span>
          </div>
          {filterOpen && (
            <div style={{ padding: '0 20px 16px' }}>
              <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-neutral-600)', marginBottom: 6 }}>가족</div>
              <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
                {familyNames.map((name) => (
                  <span
                    key={name}
                    data-testid={`family-toggle-${name}`}
                    style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 'var(--font-size-xs)', cursor: 'pointer' }}
                    onClick={() => toggleFamily(name)}
                  >
                    <span className={`chk${family[name] ? ' on' : ''}`}>{family[name] ? '✓' : ''}</span>
                    {name}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="loan-list">
          {items.length > 0 && (
            <div className="loan-list-full" style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-neutral-600)', fontWeight: 'var(--font-weight-medium)' }}>
              총 {items.length}건
            </div>
          )}
          {tab === 'current'
            ? filteredMutualLoans.map((m) => <MutualLoanCard key={m.id} mutualLoan={m} />)
            : filteredHistory.map((h) => <MutualLoanHistoryCard key={h.id} item={h} />)}
          {items.length === 0 && (
            <div className="loan-list-full">
              <EmptyState message="조회되는 도서가 없습니다" />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
