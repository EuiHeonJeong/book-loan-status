// 백엔드 DTO(backend/src/main/java/com/woori/library/dto)와 1:1로 대응하는 API 전송 타입.
// 프론트 도메인 타입(../types.ts)과는 id 타입(number vs string), 선택적 필드 등이 달라 분리한다.

export interface MeResponse {
  userId: number;
  name: string;
  provider: string;
}

export interface LibraryAccountResponse {
  id: number;
  loginId: string;
}

export interface MemberResponse {
  id: number;
  name: string;
  isSelf: boolean;
  libraryAccounts: LibraryAccountResponse[];
}

export interface LoanResponse {
  title: string;
  loanDate: string;
  dueDate: string;
  library: string;
  memberName: string;
  dday: number;
  overdue: boolean;
}

export interface SyncResponse {
  synced: number;
  failed: string[];
}

export interface ReservationResponse {
  title: string;
  branch: string;
  reservedAt: string;
  expiresAt: string | null;
  rank: number | null;
  statusText: string;
  ready: boolean;
  memberName: string;
}

export interface MutualLoanResponse {
  title: string;
  branch: string;
  pickupBranch: string;
  appliedAt: string;
  statusText: string;
  ready: boolean;
  memberName: string;
}

export interface MutualLoanHistoryResponse {
  title: string;
  branch: string;
  pickupBranch: string;
  appliedAt: string;
  statusText: string;
  memberName: string;
}

export interface NotificationSettingResponse {
  dueAlertEnabled: boolean;
  dueAlertTiming: 'd3' | 'd2' | 'd1' | 'd0';
}

export interface CreateMemberRequest {
  name: string;
}

export interface UpdateMemberRequest {
  name: string;
}

export interface CreateLibraryAccountRequest {
  loginId: string;
  password: string;
}

export interface UpdateLibraryAccountRequest {
  loginId?: string;
  password?: string;
}

export interface NotificationSettingRequest {
  dueAlertEnabled?: boolean;
  dueAlertTiming?: 'd3' | 'd2' | 'd1' | 'd0';
}

export interface PushSubscriptionRequest {
  endpoint: string;
  keys: { p256dh: string; auth: string };
}
