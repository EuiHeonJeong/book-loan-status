import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AppBar } from '../components/AppBar';
import { BackIcon } from '../components/icons';
import { ReservationCard } from '../components/ReservationCard';
import { EmptyState } from '../components/EmptyState';
import { getReservations } from '../api/reservations';
import { listMembers } from '../api/members';
import type { ReservationResponse } from '../api/types';
import { LIBRARY_CODES, type LibraryCode } from '../types';
import type { ReservationItem } from '../types';

function toReservationItem(r: ReservationResponse, index: number): ReservationItem {
  return {
    id: `${r.title}-${r.reservedAt}-${r.memberName}-${index}`,
    title: r.title,
    branch: r.branch,
    reservedAt: r.reservedAt,
    expiresAt: r.expiresAt,
    rank: r.rank,
    statusText: r.statusText,
    ready: r.ready,
    memberName: r.memberName,
  };
}

export function ReservationsPage() {
  const navigate = useNavigate();
  const [filterOpen, setFilterOpen] = useState(false);
  const [familyNames, setFamilyNames] = useState<string[]>([]);
  const [family, setFamily] = useState<Record<string, boolean>>({});
  const [libraries, setLibraries] = useState<Record<LibraryCode, boolean>>(
    Object.fromEntries(LIBRARY_CODES.map((c) => [c, true])) as Record<LibraryCode, boolean>
  );
  const [reservations, setReservations] = useState<ReservationItem[]>([]);

  useEffect(() => {
    listMembers().then((members) => {
      const names = members.map((m) => m.name);
      setFamilyNames(names);
      setFamily(Object.fromEntries(names.map((n) => [n, true])));
    });
    getReservations().then((data) => setReservations(data.map(toReservationItem)));
  }, []);

  const toggleFamily = (name: string) => setFamily((f) => ({ ...f, [name]: !f[name] }));
  const toggleLibrary = (code: LibraryCode) => setLibraries((l) => ({ ...l, [code]: !l[code] }));

  const filtered = useMemo(
    () => reservations.filter((r) => family[r.memberName] !== false && libraries[r.branch as LibraryCode] !== false),
    [reservations, family, libraries]
  );

  return (
    <div className="phone">
      <AppBar
        title="일반예약현황"
        leading={
          <button className="icon-btn" onClick={() => navigate(-1)}>
            <BackIcon />
          </button>
        }
      />
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
            </div>
          )}
        </div>

        <div className="loan-list">
          {filtered.length > 0 && (
            <div className="loan-list-full" style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-neutral-600)', fontWeight: 'var(--font-weight-medium)' }}>
              총 {filtered.length}건
            </div>
          )}
          {filtered.map((r) => (
            <ReservationCard key={r.id} reservation={r} />
          ))}
          {filtered.length === 0 && (
            <div className="loan-list-full">
              <EmptyState message="조회되는 도서가 없습니다" />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
