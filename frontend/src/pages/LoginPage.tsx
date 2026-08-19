import { useState } from 'react';
import { oauthLoginUrl, type OAuthProvider } from '../api/auth';
import { GoogleLogo, NaverLogo, RefreshIcon } from '../components/icons';

// 네이버는 검수 전이라 테스트 계정만 로그인 가능 — 검수 완료 전까지 버튼을 숨긴다.
const NAVER_LOGIN_ENABLED = false;

export function LoginPage() {
  const [loadingProvider, setLoadingProvider] = useState<OAuthProvider | null>(null);

  // Spring Security의 oauth2Login이 처리하는 전체 페이지 리다이렉트 — SPA 라우팅이 아니라 location 이동.
  // 리다이렉트가 실제로 시작되기까지 지연이 있을 수 있어(특히 구글), 그 사이 버튼을 로딩 상태로 바꾼다.
  const handleLogin = (provider: OAuthProvider) => {
    setLoadingProvider(provider);
    location.href = oauthLoginUrl(provider);
  };

  return (
    <div className="phone" style={{ alignItems: 'center', justifyContent: 'center', gap: 36, padding: '0 32px' }}>
      <div style={{ textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
        <div style={{ fontFamily: 'var(--font-display)', fontSize: 'var(--font-size-2xl)', color: 'var(--color-primary-700)' }}>
          우리서재
        </div>
        <div style={{ fontSize: 'var(--font-size-lg)', fontWeight: 'var(--font-weight-bold)', color: 'var(--color-text)' }}>
          대출 현황 조회
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 14, width: '100%' }}>
        <button
          onClick={() => handleLogin('google')}
          disabled={loadingProvider !== null}
          style={{
            height: 52,
            borderRadius: 'var(--radius-pill)',
            border: '1.5px solid var(--color-neutral-400)',
            background: 'var(--color-neutral-0)',
            boxShadow: 'var(--shadow-sm)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 10,
            fontSize: 'var(--font-size-base)',
            fontWeight: 'var(--font-weight-bold)',
            color: 'var(--color-neutral-900)',
            cursor: loadingProvider !== null ? 'default' : 'pointer',
            opacity: loadingProvider !== null && loadingProvider !== 'google' ? 0.5 : 1,
          }}
        >
          {loadingProvider === 'google' ? (
            <>
              <RefreshIcon spinning />
              이동 중...
            </>
          ) : (
            <>
              <GoogleLogo />
              구글로 로그인
            </>
          )}
        </button>
        {NAVER_LOGIN_ENABLED && (
          <button
            onClick={() => handleLogin('naver')}
            disabled={loadingProvider !== null}
            style={{
              height: 52,
              borderRadius: 'var(--radius-pill)',
              border: 'none',
              background: '#03C75A',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 10,
              fontSize: 'var(--font-size-base)',
              fontWeight: 'var(--font-weight-bold)',
              color: '#fff',
              cursor: loadingProvider !== null ? 'default' : 'pointer',
              opacity: loadingProvider !== null && loadingProvider !== 'naver' ? 0.5 : 1,
            }}
          >
            {loadingProvider === 'naver' ? (
              <>
                <RefreshIcon spinning />
                이동 중...
              </>
            ) : (
              <>
                <NaverLogo />
                네이버로 로그인
              </>
            )}
          </button>
        )}
      </div>

      <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-neutral-600)' }}>※ 소셜 로그인만 지원</div>
    </div>
  );
}
