import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AppBar } from '../components/AppBar';
import { BackIcon } from '../components/icons';
import { ReservationCard } from '../components/ReservationCard';
import { EmptyState } from '../components/EmptyState';
import { getReservations } from '../api/reservations';
import type { ReservationResponse } from '../api/types';
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
  const [reservations, setReservations] = useState<ReservationItem[]>([]);

  useEffect(() => {
    getReservations().then((data) => setReservations(data.map(toReservationItem)));
  }, []);

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
      <div className="loan-list" style={{ flex: 1, overflowY: 'auto' }}>
        {reservations.length > 0 && (
          <div className="loan-list-full" style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-neutral-600)', fontWeight: 'var(--font-weight-medium)' }}>
            총 {reservations.length}건
          </div>
        )}
        {reservations.map((r) => (
          <ReservationCard key={r.id} reservation={r} />
        ))}
        {reservations.length === 0 && (
          <div className="loan-list-full">
            <EmptyState message="조회되는 도서가 없습니다" />
          </div>
        )}
      </div>
    </div>
  );
}
