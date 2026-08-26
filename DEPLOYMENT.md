# 배포 가이드

프론트는 Vercel, 백엔드+DB는 집 PC에 올린 VirtualBox Ubuntu Server VM에 Docker Compose로 셀프호스팅한다. 외부 노출은 포트포워딩이 아니라 **Cloudflare Tunnel**(아웃바운드 전용 연결)로 하고, 구매한 도메인 `my-library.org`를 쓴다. 배포는 자동화(CI) 없이 VM에서 `deploy/deploy.sh`를 수동 실행하는 방식이다.

> Oracle Cloud Always Free로 시작하려 했으나 계정 생성 단계에서 계속 실패해 포기하고, 여분의 집 PC(VirtualBox)로 전환했다. VM 최초 설치(VirtualBox 설치·netplan 고정 IP·Task Scheduler 자동시작 등)의 상세 단계는 별도 아티팩트 가이드에 정리되어 있고, 이 문서는 **현재 운영 구조와 재배포 절차** 위주로 다룬다.

## 아키텍처

```
[사용자 브라우저] ──HTTPS──▶ [Vercel: 프론트 정적 빌드]
        │                              │
        │                     axios(withCredentials)
        ▼                              ▼
[my-library.org] ──Cloudflare Tunnel(아웃바운드)──▶ [집 PC: VirtualBox Ubuntu VM]
                                                        └─ docker compose
                                                             ├─ cloudflared (터널 클라이언트)
                                                             ├─ backend (Spring Boot, 호스트에 포트 노출 안 함)
                                                             └─ postgres (호스트/외부에 전혀 노출 안 함)
```

- Cloudflare Tunnel은 VM이 Cloudflare로 **아웃바운드 연결만** 하므로 공유기 포트포워딩·고정 공인 IP·DDNS가 전혀 필요 없다(아파트 공유 인터넷이 IPv6 우선이라 포트포워딩 자체가 막혀 있었음 — 이게 Tunnel을 최종 채택한 이유).
- `postgres`는 `docker-compose.yml`에 포트를 아예 노출하지 않는다 — `backend` 컨테이너만 내부 도커 네트워크로 접근.

## VM(최초 1회, 이미 완료됨)

- 여분의 Windows PC에 VirtualBox(무료) 설치 → Ubuntu Server VM(2 vCPU / 3GB RAM / 40GB, **Bridged Adapter** 네트워크).
- 고정 IP는 공유기 DHCP 예약이 아니라 **Ubuntu netplan**으로 설정(재부팅해도 절대 안 바뀜).
- Docker Engine + Compose plugin 설치:
  ```bash
  sudo apt-get update && sudo apt-get install -y ca-certificates curl gnupg
  sudo install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  sudo chmod a+r /etc/apt/keyrings/docker.gpg
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list
  sudo apt-get update
  sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  sudo usermod -aG docker "$USER"
  sudo systemctl enable --now docker
  ```
- 저장소 클론: `git clone https://github.com/EuiHeonJeong/book-loan-status.git ~/book-loan-status`(private이면 Deploy key 등록 후 SSH clone).
- **재부팅 복원력**(호스트 Windows Task Scheduler, "시작할 때" 트리거):
  ```
  "C:\Program Files\Oracle\VirtualBox\VBoxManage.exe" startvm "<VM이름>" --type headless
  ```
  Docker는 `systemctl enable --now docker`로 부팅 시 자동 시작, 컨테이너는 `restart: unless-stopped`로 자동 복귀 — VM만 뜨면 사람 개입 없이 전부 복구된다.

## Cloudflare Tunnel 설정(최초 1회, 이미 완료됨)

1. Cloudflare에서 도메인 `my-library.org` 구매·연결.
2. Cloudflare Zero Trust 대시보드 → Networks → Tunnels → 터널 생성 → "Docker" 설치 방법 선택 시 발급되는 토큰을 `deploy/.env`의 `CLOUDFLARE_TUNNEL_TOKEN`에 저장.
3. 같은 화면에서 Public Hostname 매핑: `my-library.org` → Service `http://backend:8080`(docker compose 내부 DNS 이름).
4. 80/443 포트를 VM이나 공유기에서 열 필요가 전혀 없다 — `cloudflared` 컨테이너가 전부 처리.

## `deploy/.env` (VM에서만 만든다, 절대 커밋 금지)

`deploy/.env.example`을 복사해서 채운다 — 필드 설명은 그 파일 주석 참고. 핵심만 요약:

- `CLOUDFLARE_TUNNEL_TOKEN` — 위 단계에서 발급된 값
- `DB_NAME=woori_library`, `DB_USER=app`, `DB_PASSWORD=`(강력한 값)
- `DB_URL=jdbc:postgresql://postgres:5432/woori_library` — `env_file`은 변수 치환이 안 되므로 `DB_NAME`과 같은 값을 literal로 적을 것
- `LIBRARY_PW_ENC_KEY` — 로컬 `.env`와 반드시 동일한 값(다르면 기존에 저장된 가족 도서관 계정 비밀번호를 복호화 못 해 전부 재등록해야 함)
- `OAUTH_GOOGLE_CLIENT_ID/SECRET`, `OAUTH_NAVER_CLIENT_ID/SECRET`, `VAPID_PUBLIC_KEY/PRIVATE_KEY/SUBJECT` — 로컬 `.env`와 동일
- `APP_FRONTEND_BASE_URL` — Vercel 배포 도메인(끝에 `/` 없이)
- `SESSION_COOKIE_SAME_SITE=none`, `SESSION_COOKIE_SECURE=true` — 프론트(Vercel)와 백엔드(my-library.org)가 서로 다른 도메인이라 필수. 없으면 로그인은 되는데 이후 API 요청에 세션 쿠키가 안 실려가 전부 401 처리됨.

## Vercel(프론트, 최초 1회, 이미 완료됨)

1. New Project → GitHub 저장소(`book-loan-status`) import, **Root Directory**를 `frontend`로 지정(Vite 자동 인식)
2. 환경변수: `VITE_API_BASE_URL=https://my-library.org`, `VITE_VAPID_PUBLIC_KEY`(로컬 `.env`의 `VAPID_PUBLIC_KEY`와 동일)
3. 이후 `main` 브랜치 push 시 자동 재배포됨(별도 조작 불필요)

## Google Cloud Console / Naver Developers — OAuth 리다이렉트 URI(최초 1회, 이미 완료됨)

- Google Cloud Console 승인된 리디렉션 URI: `https://my-library.org/login/oauth2/code/google`
- Naver Developers 콜백 URL: `https://my-library.org/login/oauth2/code/naver`(현재 정식 검수 대신 "멤버 관리"에 등록한 테스트 계정만 로그인 가능 — 프론트 로그인 버튼도 숨겨둔 상태, `LoginPage.tsx`의 `NAVER_LOGIN_ENABLED`)

## 재배포 (코드 수정 후 — 실질적으로 이 문서를 열어볼 이유의 대부분)

```bash
ssh <사용자>@<VM 고정 IP>          # 같은 집 네트워크 안에서만 접속 가능(VM은 외부에 SSH 노출 안 함)
cd ~/book-loan-status/deploy
./deploy.sh
```

`deploy.sh`가 `git pull --ff-only` → `docker compose up -d --build` → 이미지 정리까지 한 번에 처리한다. 프론트는 Vercel이 push 시 알아서 재배포하므로 손댈 게 없다.

**확인**: `docker compose -f deploy/docker-compose.yml ps`로 `postgres`/`backend`/`cloudflared` 3개 컨테이너가 전부 `Up`인지, `docker compose -f deploy/docker-compose.yml logs -f backend`로 Flyway 마이그레이션이 에러 없이 적용됐는지(`Migrating schema "app" to version "N"` 로그) 확인.

## 알려진 제약

- **세션이 in-memory라 백엔드 재배포/재시작마다 로그인 중이던 모든 사용자가 로그아웃된다.** 문제가 되면 Redis 등 영속 세션 스토어 도입을 고려할 것.
- `PushNotificationScheduler`/`LoanSyncService`에 동시 실행 방지 장치가 없다. 개인용 앱 규모에선 위험이 낮지만 계정이 늘어나면 재검토할 것.
- Naver는 정식 검수 대신 테스트 계정 등록 방식이라, 등록 안 된 구글 계정으로만 실사용 중(로그인 버튼도 숨김).
- VM 자체(OS 보안 패치, 디스크 사용량, Docker/Postgres 업그레이드)는 관리형 서비스가 아니라 직접 관리해야 한다 — `docker image prune -f`는 `deploy.sh`가 매번 해주지만, 디스크가 가득 차지 않는지 가끔 `df -h`로 확인할 것.
- VM은 집 네트워크 안에서만 SSH 접속 가능(외부 노출 없음) — 다른 곳에서 배포하려면 VPN 등으로 집 네트워크에 먼저 들어와야 한다.
