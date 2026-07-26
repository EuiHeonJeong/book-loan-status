import { apiClient } from './client';
import type {
  CreateLibraryAccountRequest,
  CreateMemberRequest,
  LibraryAccountResponse,
  MemberResponse,
  UpdateLibraryAccountRequest,
  UpdateMemberRequest,
} from './types';

export async function listMembers(): Promise<MemberResponse[]> {
  const { data } = await apiClient.get<MemberResponse[]>('/api/members');
  return data;
}

export async function createMember(request: CreateMemberRequest): Promise<MemberResponse> {
  const { data } = await apiClient.post<MemberResponse>('/api/members', request);
  return data;
}

export async function updateMember(id: number, request: UpdateMemberRequest): Promise<MemberResponse> {
  const { data } = await apiClient.put<MemberResponse>(`/api/members/${id}`, request);
  return data;
}

export async function deleteMember(id: number): Promise<void> {
  await apiClient.delete(`/api/members/${id}`);
}

export async function addLibraryAccount(
  memberId: number,
  request: CreateLibraryAccountRequest
): Promise<LibraryAccountResponse> {
  const { data } = await apiClient.post<LibraryAccountResponse>(
    `/api/members/${memberId}/library-accounts`,
    request
  );
  return data;
}

export async function updateLibraryAccount(
  accountId: number,
  request: UpdateLibraryAccountRequest
): Promise<LibraryAccountResponse> {
  const { data } = await apiClient.put<LibraryAccountResponse>(`/api/library-accounts/${accountId}`, request);
  return data;
}
