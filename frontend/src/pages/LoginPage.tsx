import { oauthLoginUrl, type OAuthProvider } from '../api/auth';
import { GoogleLogo, NaverLogo } from '../components/icons';

export function LoginPage() {
  // Spring Security의 oauth2Login이 처리하는 전체 페이지 리다이렉트 — SPA 라우팅이 아니라 location 이동.
  const handleLogin = (provider: OAuthProvider) => {
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
            cursor: 'pointer',
          }}
        >
          <GoogleLogo />
          구글로 로그인
        </button>
        <button
          onClick={() => handleLogin('naver')}
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
            cursor: 'pointer',
          }}
        >
          <NaverLogo />
          네이버로 로그인
        </button>
      </div>

      <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-neutral-600)' }}>※ 소셜 로그인만 지원</div>
    </div>
  );
}
