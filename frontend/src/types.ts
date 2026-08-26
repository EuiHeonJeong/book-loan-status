export const LIBRARY_CODES = [
  '검암',
  '아라누리',
  '단봉늘봄',
  '검단',
  '심곡',
  '석남',
  '신석',
] as const;

export type LibraryCode = (typeof LIBRARY_CODES)[number];

export interface LibraryAccount {
  id: string;
  loginId: string;
}

export interface FamilyMember {
  id: string;
  name: string;
  isSelf: boolean;
  libraryAccounts: LibraryAccount[];
}

export interface LoanRecord {
  id: string;
  title: string;
  loanDate: string; // MM.DD
  dueDate: string; // MM.DD
  library: LibraryCode;
  memberName: string;
  dday: number; // negative = overdue
}

export type SortKey = 'due' | 'loan';
export type SortDir = 'asc' | 'desc';

export interface NotificationSettings {
  dueAlertEnabled: boolean;
  dueAlertTiming: 'd3' | 'd2' | 'd1' | 'd0';
}

export interface ReservationItem {
  id: string;
  title: string;
  branch: string;
  reservedAt: string; // MM.DD
  expiresAt: string | null; // MM.DD
  rank: number | null;
  statusText: string;
  ready: boolean;
  memberName: string;
}

export interface MutualLoanItem {
  id: string;
  title: string;
  branch: string;
  pickupBranch: string;
  appliedAt: string; // MM.DD
  statusText: string;
  ready: boolean;
  memberName: string;
}

export interface MutualLoanHistoryItem {
  id: string;
  title: string;
  branch: string;
  pickupBranch: string;
  appliedAt: string; // MM.DD
  statusText: string;
  memberName: string;
}
