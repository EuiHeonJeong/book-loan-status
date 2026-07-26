import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { AppBar } from '../components/AppBar';
import { BackIcon, EyeIcon, EyeOffIcon } from '../components/icons';
import {
  addLibraryAccount,
  createMember,
  deleteMember,
  listMembers,
  updateLibraryAccount,
  updateMember,
} from '../api/members';
import type { MemberResponse } from '../api/types';

interface EditDraft {
  name: string;
  loginId: string;
  password: string;
}

const emptyDraft = (m: MemberResponse): EditDraft => ({
  name: m.name,
  loginId: m.libraryAccounts[0]?.loginId ?? '',
  password: '',
});

export function FamilyMembersPage() {
  const navigate = useNavigate();
  const [members, setMembers] = useState<MemberResponse[]>([]);
  const [editing, setEditing] = useState<number | null>(null);
  const [draft, setDraft] = useState<EditDraft>({ name: '', loginId: '', password: '' });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);

  useEffect(() => {
    listMembers().then(setMembers);
  }, []);

  const startEdit = (m: MemberResponse) => {
    setError(null);
    setEditing(m.id);
    setDraft(emptyDraft(m));
    setShowPassword(false);
  };

  const cancelEdit = () => {
    setError(null);
    setEditing(null);
  };

  const saveEdit = async (id: number) => {
    const member = members.find((m) => m.id === id);
    if (!member) return;

    setSaving(true);
    setError(null);
    try {
      let updated = member;
      if (draft.name !== member.name) {
        updated = await updateMember(id, { name: draft.name });
      }

      const account = member.libraryAccounts[0];
      const loginIdChanged = draft.loginId !== (account?.loginId ?? '');
      if (!account) {
        if (draft.loginId && draft.password) {
          const created = await addLibraryAccount(id, { loginId: draft.loginId, password: draft.password });
          updated = { ...updated, libraryAccounts: [created] };
        }
      } else if (draft.password || loginIdChanged) {
        const request =
          draft.password.length > 0
            ? { loginId: draft.loginId, password: draft.password }
            : { loginId: draft.loginId };
        const saved = await updateLibraryAccount(account.id, request);
        updated = { ...updated, libraryAccounts: [saved] };
      }

      setMembers((prev) => prev.map((m) => (m.id === id ? updated : m)));
      setEditing(null);
    } catch (e) {
      if (axios.isAxiosError(e) && e.response?.status === 422) {
        setError((e.response.data as { message?: string } | undefined)?.message ?? '도서관 계정 로그인 검증에 실패했습니다.');
      } else {
        setError('저장에 실패했습니다. 잠시 후 다시 시도해주세요.');
      }
    } finally {
      setSaving(false);
    }
  };

  const removeMember = async (id: number) => {
    try {
      await deleteMember(id);
      setMembers((prev) => prev.filter((m) => m.id !== id));
    } catch {
      setError('삭제에 실패했습니다. 잠시 후 다시 시도해주세요.');
    }
  };

  const addMember = async () => {
    setError(null);
    try {
      const created = await createMember({ name: '새 구성원' });
      setMembers((prev) => [...prev, created]);
      startEdit(created);
    } catch {
      setError('구성원 추가에 실패했습니다. 잠시 후 다시 시도해주세요.');
    }
  };

  return (
    <div className="phone">
      <AppBar
        title="가족 관리"
        leading={
          <button className="icon-btn" onClick={() => navigate(-1)}>
            <BackIcon />
          </button>
        }
      />
      <div style={{ flex: 1, overflowY: 'auto', padding: '16px 20px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        {members.map((m) => {
          const isEditing = editing === m.id;
          const disabled = editing !== null && editing !== m.id;
          const loginId = m.libraryAccounts[0]?.loginId ?? '';
          return (
            <div
              key={m.id}
              data-testid={`member-card-${m.id}`}
              style={{
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-md)',
                padding: 16,
                display: 'flex',
                flexDirection: 'column',
                gap: 10,
                opacity: disabled ? 0.5 : 1,
                pointerEvents: disabled ? 'none' : 'auto',
                background: disabled ? 'var(--color-neutral-50)' : 'var(--color-bg-surface)',
                borderColor: isEditing ? 'var(--color-primary-700)' : 'var(--color-border)',
                borderWidth: isEditing ? 2 : 1,
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
                <div className={`input-field${isEditing ? ' editing' : ''}`} style={{ flex: 1 }}>
                  <span className="field-label">이름</span>
                  {isEditing ? (
                    <input
                      data-testid={`name-input-${m.id}`}
                      value={draft.name}
                      onChange={(e) => setDraft((d) => ({ ...d, name: e.target.value }))}
                    />
                  ) : (
                    <span>{m.name}</span>
                  )}
                </div>
                {!m.isSelf && !isEditing && (
                  <button className="icon-btn sm" onClick={() => removeMember(m.id)}>
                    ✕
                  </button>
                )}
              </div>

              <div className={`input-field${isEditing ? ' editing' : ''}`}>
                <span className="field-label">아이디</span>
                {isEditing ? (
                  <input value={draft.loginId} onChange={(e) => setDraft((d) => ({ ...d, loginId: e.target.value }))} />
                ) : (
                  <span>{loginId}</span>
                )}
              </div>
              <div className={`input-field${isEditing ? ' editing' : ''}`}>
                <span className="field-label">비밀번호</span>
                {isEditing ? (
                  <>
                    <input
                      type={showPassword ? 'text' : 'password'}
                      value={draft.password}
                      onChange={(e) => setDraft((d) => ({ ...d, password: e.target.value }))}
                      placeholder="새 비밀번호 입력"
                      style={{ flex: 1 }}
                    />
                    <button
                      type="button"
                      className="icon-btn sm"
                      style={{ background: 'none' }}
                      onClick={() => setShowPassword((v) => !v)}
                      aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 보기'}
                    >
                      {showPassword ? <EyeOffIcon /> : <EyeIcon />}
                    </button>
                  </>
                ) : (
                  <span>••••••••</span>
                )}
              </div>

              {isEditing && error && (
                <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-accent-red)' }}>{error}</div>
              )}

              {isEditing ? (
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                  <button className="pill-btn" onClick={cancelEdit} disabled={saving}>
                    취소
                  </button>
                  <button
                    className="pill-btn selected"
                    data-testid={`save-${m.id}`}
                    onClick={() => saveEdit(m.id)}
                    disabled={saving}
                  >
                    {saving ? '확인 중...' : '저장'}
                  </button>
                </div>
              ) : (
                <div style={{ textAlign: 'right' }}>
                  <span
                    data-testid={`edit-${m.id}`}
                    style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-link)', cursor: 'pointer' }}
                    onClick={() => startEdit(m)}
                  >
                    수정
                  </span>
                </div>
              )}
            </div>
          );
        })}
        <div
          style={{
            border: '1px dashed var(--color-neutral-400)',
            borderRadius: 'var(--radius-md)',
            padding: 16,
            textAlign: 'center',
            fontSize: 'var(--font-size-sm)',
            color: 'var(--color-neutral-600)',
            cursor: 'pointer',
          }}
          onClick={addMember}
        >
          ＋ 가족 구성원 추가
        </div>
        {editing === null && error && (
          <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-accent-red)', textAlign: 'center' }}>{error}</div>
        )}
      </div>
    </div>
  );
}
