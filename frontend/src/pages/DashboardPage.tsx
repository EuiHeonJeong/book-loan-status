import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AppBar } from '../components/AppBar';
import { BellIcon, ChevronLeftIcon, ChevronRightIcon, MenuIcon, RefreshIcon } from '../components/icons';
import { LoanCard, UrgentLoanCard } from '../components/LoanCard';
import { EmptyState } from '../components/EmptyState';
import { LIBRARY_CODES, type LibraryCode, type LoanRecord, type SortDir, type SortKey } from '../types';
import { getLoans, syncLoans } from '../api/loans';
import { listMembers } from '../api/members';
import { logout } from '../api/auth';
import { getNotificationSettings } from '../api/notificationSettings';
import type { LoanResponse, NotificationSettingResponse } from '../api/types';

const MIN_SYNC_INDICATOR_MS = 500;
const DUE_TIMING_THRESHOLD: Record<NotificationSettingResponse['dueAlertTiming'], number> = {
  d3: 3,
  d2: 2,
  d1: 1,
  d0: 0,
};
// 접속해 있는 동안 준실시간 갱신 주기 — issl.go.kr에 매번 실제 브라우저로 로그인해서 긁어오는 방식이라
// 너무 짧게 잡으면 도서관 사이트에 부담(+ 계정 잠금 위험)을 줄 수 있어 5분으로 둔다.
const AUTO_SYNC_INTERVAL_MS = 5 * 60 * 1000;

function toLoanRecord(loan: LoanResponse, index: number): LoanRecord {
  return {
    id: `${loan.title}-${loan.loanDate}-${loan.memberName}-${index}`,
    title: loan.title,
    loanDate: loan.loanDate,
    dueDate: loan.dueDate,
    library: loan.library as LibraryCode,
    memberName: loan.memberName,
    dday: loan.dday,
  };
}

export function DashboardPage() {
  const navigate = useNavigate();
  const [filterOpen, setFilterOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [notifOpen, setNotifOpen] = useState(false);
  const [familyNames, setFamilyNames] = useState<string[]>([]);
  const [family, setFamily] = useState<Record<string, boolean>>({});
  const [libraries, setLibraries] = useState<Record<LibraryCode, boolean>>(
    Object.fromEntries(LIBRARY_CODES.map((c) => [c, true])) as Record<LibraryCode, boolean>
  );
  const [sort, setSort] = useState<SortKey>('due');
  const [sortDir, setSortDir] = useState<SortDir>('asc');
  const [syncing, setSyncing] = useState(false);
  const [lastSynced, setLastSynced] = useState<string | null>(null);
  const [loans, setLoans] = useState<LoanRecord[]>([]);
  const [notifSettings, setNotifSettings] = useState<NotificationSettingResponse>({
    dueAlertEnabled: true,
    dueAlertTiming: 'd3',
  });
  const urgentRef = useRef<HTMLDivElement>(null);
  const scrollUrgent = (dir: number) => urgentRef.current?.scrollBy({ left: dir * 140, behavior: 'smooth' });
  const syncingRef = useRef(false);

  const loadLoans = () => getLoans().then((data) => setLoans(data.map(toLoanRecord)));

  useEffect(() => {
    listMembers().then((members) => {
      const names = members.map((m) => m.name);
      setFamilyNames(names);
      setFamily(Object.fromEntries(names.map((n) => [n, true])));
    });
    loadLoans();
    getNotificationSettings().then(setNotifSettings);
  }, []);

  const refresh = () => {
    if (syncingRef.current) return;
    syncingRef.current = true;
    setSyncing(true);
    const startedAt = Date.now();
    syncLoans()
      .then(() => loadLoans())
      .then(() => {
        const now = new Date();
        const pad = (n: number) => String(n).padStart(2, '0');
        setLastSynced(`${pad(now.getMonth() + 1)}.${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}`);
      })
      .finally(() => {
        const elapsed = Date.now() - startedAt;
        setTimeout(() => {
          syncingRef.current = false;
          setSyncing(false);
        }, Math.max(0, MIN_SYNC_INDICATOR_MS - elapsed));
      });
  };

  useEffect(() => {
    refresh(); // 대시보드 진입(로그인 직후 포함) 시 최신 대출현황으로 자동 갱신
    const interval = setInterval(refresh, AUTO_SYNC_INTERVAL_MS);
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const toggleFamily = (name: string) => setFamily((f) => ({ ...f, [name]: !f[name] }));
  const toggleLibrary = (code: LibraryCode) => setLibraries((l) => ({ ...l, [code]: !l[code] }));

  const filteredLoans = useMemo(() => {
    const dir = sortDir === 'asc' ? 1 : -1;
    const rows = loans.filter((l) => family[l.memberName] !== false && libraries[l.library]);
    return [...rows].sort((a, b) => {
      const key = sort === 'due' ? 'dueDate' : 'loanDate';
      return a[key].localeCompare(b[key]) * dir;
    });
  }, [loans, family, libraries, sort, sortDir]);

  const dueThreshold = DUE_TIMING_THRESHOLD[notifSettings.dueAlertTiming];
  const urgentLoans = useMemo(
    () => (notifSettings.dueAlertEnabled ? loans.filter((l) => l.dday <= dueThreshold).slice(0, 3) : []),
    [loans, notifSettings.dueAlertEnabled, dueThreshold]
  );
  const notifications = urgentLoans;

  const closeAllOverlays = () => {
    setMenuOpen(false);
    setNotifOpen(false);
  };

  return (
    <div className="phone" onClick={() => {}}>
      <AppBar
        title="도서대여 현황"
        trailing={
          <>
            <button
              className="icon-btn"
              data-testid="notif-button"
              onClick={(e) => {
                e.stopPropagation();
                setNotifOpen((v) => !v);
                setMenuOpen(false);
              }}
            >
              <BellIcon />
              {notifications.length > 0 && (
                <span
                  style={{
                    position: 'absolute',
                    top: 6,
                    right: 6,
                    width: 8,
                    height: 8,
                    borderRadius: '50%',
                    background: 'var(--color-accent-red)',
                  }}
                />
              )}
            </button>
            <button
              className="icon-btn"
              data-testid="refresh-button"
              onClick={refresh}
              disabled={syncing}
              title={syncing ? '새로고침 중...' : '새로고침'}
              style={{ opacity: syncing ? 0.5 : 1 }}
            >
              <RefreshIcon spinning={syncing} />
            </button>
            <button
              className="icon-btn"
              data-testid="menu-button"
              onClick={(e) => {
                e.stopPropagation();
                setMenuOpen((v) => !v);
                setNotifOpen(false);
              }}
            >
              <MenuIcon />
            </button>
          </>
        }
      />

      <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column' }}>
        {loans.length > 0 && (
          <div style={{ padding: '16px 20px 12px', borderBottom: '1px solid var(--color-border)' }}>
            <div style={{ fontSize: 'var(--font-size-xs)', fontWeight: 'var(--font-weight-bold)', color: 'var(--color-accent-red)', marginBottom: 10 }}>
              ⚠ 반납임박 도서
            </div>
            {!notifSettings.dueAlertEnabled ? (
              <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-neutral-600)' }}>반납예정 알림이 꺼져있어요.</div>
            ) : urgentLoans.length === 0 ? (
              <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-neutral-600)' }}>반납임박 도서가 없어요.</div>
            ) : (
              <div style={{ display: 'flex', alignItems: 'stretch', gap: 6 }}>
                <button
                  className="icon-btn"
                  onClick={() => scrollUrgent(-1)}
                  style={{
                    width: 25,
                    borderRadius: 'var(--radius-sm)',
                    flexShrink: 0,
                    background: 'var(--color-bg-surface)',
                    border: '1px solid var(--color-border)',
                    alignSelf: 'stretch',
                    height: 'auto',
                  }}
                >
                  <ChevronLeftIcon />
                </button>
                <div
                  ref={urgentRef}
                  className="scrollbar-hidden"
                  style={{
                    display: 'flex',
                    gap: 10,
                    overflowX: 'auto',
                    overflowY: 'hidden',
                    paddingBottom: 4,
                    flex: 1,
                    scrollBehavior: 'smooth',
                  }}
                >
                  {urgentLoans.map((loan) => (
                    <UrgentLoanCard key={loan.id} loan={loan} />
                  ))}
                </div>
                <button
                  className="icon-btn"
                  onClick={() => scrollUrgent(1)}
                  style={{
                    width: 25,
                    borderRadius: 'var(--radius-sm)',
                    flexShrink: 0,
                    background: 'var(--color-bg-surface)',
                    border: '1px solid var(--color-border)',
                    alignSelf: 'stretch',
                    height: 'auto',
                  }}
                >
                  <ChevronRightIcon />
                </button>
              </div>
            )}
          </div>
        )}

        <div
          style={{
            position: 'sticky',
            top: 0,
            zIndex: 1,
            background: 'var(--color-bg-surface)',
            borderBottom: '1px solid var(--color-border)',
          }}
        >
          <div
            style={{ padding: '12px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer' }}
            onClick={() => setFilterOpen((v) => !v)}
          >
            <span style={{ fontSize: 'var(--font-size-sm)', fontWeight: 'var(--font-weight-bold)', color: 'var(--color-text)' }}>필터 · 정렬</span>
            <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-neutral-600)' }}>{filterOpen ? '▾ 접기' : '▸ 펼치기'}</span>
          </div>
          {filterOpen && (
            <div style={{ padding: '0 20px 16px', display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div>
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
              <div>
                <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-neutral-600)', marginBottom: 6 }}>도서관</div>
                <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
                  {LIBRARY_CODES.map((code) => (
                    <span
                      key={code}
                      data-testid={`library-toggle-${code}`}
                      style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 'var(--font-size-xs)', cursor: 'pointer' }}
                      onClick={() => toggleLibrary(code)}
                    >
                      <span className={`chk${libraries[code] ? ' on' : ''}`}>{libraries[code] ? '✓' : ''}</span>
                      {code}
                    </span>
                  ))}
                </div>
              </div>
              <div>
                <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-neutral-600)', marginBottom: 6 }}>정렬</div>
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                  {(
                    [
                      { value: 'due', label: '반납예정일' },
                      { value: 'loan', label: '대출일' },
                    ] as const
                  ).map((o) => (
                    <button
                      key={o.value}
                      className={`pill-btn${sort === o.value ? ' selected' : ''}`}
                      onClick={() => setSort(o.value)}
                    >
                      {o.label}
                    </button>
                  ))}
                  <button
                    className="pill-btn"
                    onClick={() => setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))}
                  >
                    {sortDir === 'asc' ? '오름차순 ▲' : '내림차순 ▼'}
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="loan-list">
          {filteredLoans.length > 0 && (
            <div className="loan-list-full" style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-neutral-600)', fontWeight: 'var(--font-weight-medium)' }}>
              총 {filteredLoans.length}건
            </div>
          )}
          {filteredLoans.map((loan) => (
            <LoanCard key={loan.id} loan={loan} />
          ))}
          {filteredLoans.length === 0 && (
            <div className="loan-list-full">
              <EmptyState message="대여 현황이 없습니다" />
            </div>
          )}
          {lastSynced && (
            <div className="loan-list-full" style={{ fontSize: 11, color: 'var(--color-neutral-600)', textAlign: 'center', marginTop: 6, whiteSpace: 'nowrap' }}>
              최근 갱신 {lastSynced} · 실시간 정보와 다를 수 있어요
            </div>
          )}
        </div>
      </div>

      {menuOpen && (
        <>
          <div className="overlay" onClick={closeAllOverlays} />
          <div className="dropdown" style={{ top: 60, right: 16, width: 200 }}>
            {['가족 관리', '알림 설정', '로그아웃'].map((label) => (
              <div
                key={label}
                style={{ padding: '12px 16px', fontSize: 'var(--font-size-sm)', color: 'var(--color-text)', borderBottom: '1px solid var(--color-border)', cursor: 'pointer' }}
                onClick={() => {
                  closeAllOverlays();
                  if (label === '가족 관리') navigate('/family');
                  else if (label === '알림 설정') navigate('/notifications');
                  else logout().finally(() => navigate('/login'));
                }}
              >
                {label}
              </div>
            ))}
          </div>
        </>
      )}

      {notifOpen && (
        <>
          <div className="overlay" onClick={closeAllOverlays} />
          <div className="dropdown" style={{ top: 60, right: 16, width: 270, maxHeight: 340, display: 'flex', flexDirection: 'column' }}>
            <div style={{ padding: '12px 16px', fontSize: 'var(--font-size-sm)', fontWeight: 'var(--font-weight-bold)', borderBottom: '1px solid var(--color-border)' }}>
              알림
            </div>
            <div style={{ overflowY: 'auto' }}>
              {notifications.map((n) => {
                const overdue = n.dday < 0;
                return (
                  <div key={n.id} style={{ padding: '12px 16px', borderBottom: '1px solid var(--color-border)', display: 'flex', flexDirection: 'column', gap: 4 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                      <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 'var(--font-weight-bold)', color: 'var(--color-text)' }}>{n.title}</span>
                      <span className={overdue ? 'badge overdue' : 'badge due'}>{overdue ? `연체 D+${-n.dday}` : `D-${n.dday}`}</span>
                    </div>
                    <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-neutral-600)' }}>
                      반납예정 {n.dueDate} · {n.library} · {n.memberName}
                    </div>
                  </div>
                );
              })}
              {notifications.length === 0 && <EmptyState message="새 알림이 없습니다." icon={false} />}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
