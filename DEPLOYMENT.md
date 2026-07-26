# 배포 가이드

백엔드+DB는 Railway, 프론트는 Vercel에 배포한다. 아래 단계는 브라우저 로그인이 필요해 직접 진행해야 한다.

## 1. Railway (백엔드 + Postgres)

1. Railway에서 New Project → **Deploy from GitHub repo** → `EuiHeonJeong/book-loan-status` 선택
2. 생성된 서비스 설정에서 **Root Directory**를 `backend`로 지정 — `backend/Dockerfile`을 자동 인식해서 빌드한다
3. 같은 프로젝트에 **Postgres** 플러그인 추가 (New → Database → PostgreSQL)
4. 백엔드 서비스의 Variables에 아래 값 설정:

   | 변수 | 값 |
   |---|---|
   | `DB_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
   | `DB_USER` | `${{Postgres.PGUSER}}` |
   | `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
   | `LIBRARY_PW_ENC_KEY` | 로컬 `.env`의 값 그대로 |
   | `OAUTH_GOOGLE_CLIENT_ID` / `OAUTH_GOOGLE_CLIENT_SECRET` | 로컬 `.env`의 값 그대로 |
   | `OAUTH_NAVER_CLIENT_ID` / `OAUTH_NAVER_CLIENT_SECRET` | 로컬 `.env`의 값 그대로 |
   | `VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY` / `VAPID_SUBJECT` | 로컬 `.env`의 값 그대로 |
   | `APP_FRONTEND_BASE_URL` | 아직 Vercel URL이 없으니 비워두고 5단계에서 채움 |

   `${{Postgres.PGHOST}}` 형식은 Railway의 변수 참조 문법 — 같은 프로젝트의 Postgres 플러그인이 노출하는 값을 그대로 가져다 쓴다.

5. 배포 후 Railway가 발급하는 공개 도메인(`https://xxx.up.railway.app` 형태)을 확인해둔다 — 아래 단계에서 계속 사용

## 2. Google Cloud Console — OAuth 리디렉션 URI 추가

사용 중인 OAuth 2.0 클라이언트의 **승인된 리디렉션 URI**에 추가:
```
https://<railway 도메인>/login/oauth2/code/google
```

## 3. Naver Developers — 콜백 URL 추가

애플리케이션의 서비스 URL/Callback URL에 추가:
```
https://<railway 도메인>/login/oauth2/code/naver
```

## 4. Vercel (프론트)

1. New Project → GitHub 저장소(`book-loan-status`) import
2. **Root Directory**를 `frontend`로 지정 (Framework Preset: Vite 자동 인식)
3. 환경변수 설정:
   - `VITE_API_BASE_URL` = 1단계에서 확인한 Railway 도메인
   - `VITE_VAPID_PUBLIC_KEY` = 로컬 `.env`의 `VAPID_PUBLIC_KEY`와 동일한 값
4. 배포 후 발급된 Vercel 도메인 확인

## 5. Railway로 돌아가서 마무리

`APP_FRONTEND_BASE_URL`을 4단계의 Vercel 도메인으로 채우고 재배포. (이 값이 OAuth 로그인 성공 후 리다이렉트 대상 + CORS 허용 origin으로 쓰인다.)

## 6. 최종 확인

실제 도메인으로 접속해서:
- Google/Naver 로그인이 정상 동작하는지
- 가족 도서관 계정을 등록해 issl.go.kr 실제 크롤링(컨테이너 안 Chromium)이 성공하는지 — 배포에서 가장 위험도가 높은 지점
- 알림 설정에서 "이 기기에서 푸시 알림 받기"를 켰을 때 브라우저 권한 프롬프트가 뜨고 구독이 Railway DB의 `push_subscription` 테이블에 저장되는지

## 알려진 제약
- 세션이 in-memory라 백엔드 재배포/재시작마다 로그인 중이던 모든 사용자가 로그아웃된다. 문제가 되면 Redis 등 영속 세션 스토어 도입을 고려할 것.
- `PushNotificationScheduler`/`LoanSyncService`에 동시 실행 방지 장치가 없다. 개인용 앱 규모에선 위험이 낮지만 계정이 늘어나면 재검토할 것.
