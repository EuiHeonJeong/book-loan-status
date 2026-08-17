import axios from 'axios';

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

// 백엔드는 세션 쿠키로 인증한다 (SecurityConfig 참고). 프론트와 백엔드가 서로 다른 도메인이라
// CSRF는 쿠키 더블서밋 대신 CORS(우리 origin만 허용) + withCredentials 조합으로 막는다.
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
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
