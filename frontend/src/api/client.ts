import axios from 'axios';

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

// 백엔드는 세션 쿠키 인증 + CSRF 쿠키(XSRF-TOKEN)를 사용한다 (SecurityConfig 참고).
// 개발 환경은 프론트(5173)/백엔드(8080) 오리진이 달라 axios가 기본적으로는 쿠키의
// XSRF 토큰을 헤더로 실어 보내지 않으므로 withXSRFToken을 명시해야 한다.
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  withXSRFToken: true,
});

// 세션이 없거나 만료된 상태로 API를 호출하면 401이 온다 — 로그인 페이지로 되돌린다.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && location.pathname !== '/login') {
      location.href = '/login';
    }
    return Promise.reject(error);
  }
);
