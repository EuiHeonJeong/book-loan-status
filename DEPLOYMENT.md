# 배포 가이드

백엔드+DB는 Oracle Cloud "Always Free" VM(Ampere A1, ARM64)에 Docker Compose로 직접 올리고, 프론트는 Vercel에 배포한다. 도메인을 따로 사지 않고 sslip.io 무료 서브도메인 + Caddy 자동 HTTPS를 쓴다. 배포는 자동화(CI) 없이 VM에서 `deploy/deploy.sh`를 수동 실행하는 방식이다.

아래 단계는 브라우저 로그인/SSH 접속이 필요해 직접 진행해야 한다.

## 1. Oracle Cloud — VM 생성

1. OCI 콘솔 로그인 → Compute → Instances → **Create Instance**
2. Image: **Ubuntu**(최신 LTS) / Shape: **VM.Standard.A1.Flex**(Always Free 대상)
3. **최소 2 OCPU / 12GB RAM**으로 설정 — 기본값(1 OCPU/6GB)은 Chromium+JVM+Postgres 동시 구동 시 메모리 부족 위험이 있다. Always Free 한도(4 OCPU/24GB) 안에서 조정.
4. SSH 키: 콘솔이 제공하는 키 페어를 다운로드(또는 본인 공개키 붙여넣기) — 이후 SSH 접속에 사용
5. 생성 후 **Public IP** 확인해서 기록해둘 것 (예: `123.45.67.89`)

## 2. 네트워크 오픈 — 두 군데 다 해야 함

클라우드 쪽만 열고 OS 쪽(iptables)을 안 열면 여전히 접속이 막힌다.

1. **클라우드 쪽**: 인스턴스 상세 → 연결된 VCN → Security List(또는 VM의 Network Security Group) → Ingress Rules 추가: `0.0.0.0/0` / TCP / 포트 `80`, 그리고 같은 방식으로 포트 `443`
2. **OS 쪽**: 아래 3단계에서 SSH 접속 후 진행

## 3. VM 최초 설정 (SSH 접속 후 딱 1번만)

```bash
ssh -i <다운받은키> ubuntu@<Public IP>

# --- Docker Engine + Compose plugin 설치 ---
sudo apt-get update && sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker "$USER"
sudo systemctl enable --now docker
# 여기서 한 번 로그아웃 후 재접속(exit 하고 다시 ssh) — docker 그룹 반영을 위해 필요

# --- OS 방화벽(iptables) 오픈 ---
# Oracle Ubuntu 이미지는 클라우드 Security List를 열어도 이 규칙이 남아있으면 여전히 막는다.
sudo iptables -I INPUT -p tcp -m state --state NEW --dport 80 -j ACCEPT
sudo iptables -I INPUT -p tcp -m state --state NEW --dport 443 -j ACCEPT
sudo apt-get install -y iptables-persistent   # 설치 중 "현재 규칙을 저장할까요?" 물으면 Yes
sudo netfilter-persistent save

# --- 저장소 클론 ---
# 저장소가 public이면:
git clone https://github.com/EuiHeonJeong/book-loan-status.git ~/book-loan-status
# private이면 대신 배포용 SSH 키를 만들어 GitHub repo → Settings → Deploy keys(Read-only)에 등록 후:
#   ssh-keygen -t ed25519 -C "oracle-vm-deploy" -f ~/.ssh/id_ed25519 -N ""
#   cat ~/.ssh/id_ed25519.pub   ← 이 값을 GitHub Deploy keys에 등록
#   git clone git@github.com:EuiHeonJeong/book-loan-status.git ~/book-loan-status

# --- 시크릿 파일 준비 ---
cd ~/book-loan-status/deploy
cp .env.example .env
nano .env   # 실제 값 채우기 — 아래 4단계 참고
chmod 600 .env
```

## 4. `deploy/.env` 채우기 (VM에서만 만든다, 절대 커밋 금지)

- `DOMAIN` = `<Public IP를 하이픈으로 바꾼 값>.sslip.io` — 예: IP가 `123.45.67.89`면 `123-45-67-89.sslip.io`
- `DB_NAME=woori_library`, `DB_USER=app`, `DB_PASSWORD=`(새로 강력한 값 생성)
- `DB_URL=jdbc:postgresql://postgres:5432/woori_library` — `DB_NAME`과 반드시 같은 값을 그대로 적을 것(env_file은 변수 치환이 안 됨, `postgres`는 Docker Compose 내부 DNS로 자동 해석됨)
- `LIBRARY_PW_ENC_KEY` = 로컬 `.env`와 동일한 값(이미 저장된 도서관 계정 비밀번호를 복호화하려면 반드시 같은 키여야 함) — 새로 생성하면 기존 등록된 가족 계정을 다시 등록해야 함
- `OAUTH_GOOGLE_CLIENT_ID` / `OAUTH_GOOGLE_CLIENT_SECRET`, `OAUTH_NAVER_CLIENT_ID` / `OAUTH_NAVER_CLIENT_SECRET`, `VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY` / `VAPID_SUBJECT` = 로컬 `.env`와 동일한 값
- `APP_FRONTEND_BASE_URL` = Vercel 배포 도메인(6단계에서 확인) — 아직 없으면 비워두고 나중에 채워도 됨, 끝에 `/` 없이 정확히

## 5. 첫 배포

```bash
chmod +x ~/book-loan-status/deploy/deploy.sh
~/book-loan-status/deploy/deploy.sh
```
`docker compose -f deploy/docker-compose.yml ps`로 `postgres`/`backend`/`caddy` 3개 컨테이너가 전부 `Up` 상태인지 확인. `docker compose -f deploy/docker-compose.yml logs -f backend`로 Flyway 마이그레이션이 정상 적용됐는지 확인.

## 6. Vercel (프론트)

1. New Project → GitHub 저장소(`book-loan-status`) import
2. **Root Directory**를 `frontend`로 지정 (Framework Preset: Vite 자동 인식)
3. 환경변수 설정:
   - `VITE_API_BASE_URL` = `https://<DOMAIN>` (4단계에서 정한 sslip.io 도메인)
   - `VITE_VAPID_PUBLIC_KEY` = 로컬 `.env`의 `VAPID_PUBLIC_KEY`와 동일한 값
4. 배포 후 발급된 Vercel 도메인 확인

## 7. VM으로 돌아가서 마무리

`deploy/.env`의 `APP_FRONTEND_BASE_URL`을 6단계의 Vercel 도메인으로 채우고(비워뒀다면) `~/book-loan-status/deploy/deploy.sh`를 다시 실행해 반영.

## 8. Google Cloud Console / Naver Developers — OAuth 리다이렉트 URI 등록

- Google Cloud Console: 승인된 리디렉션 URI에 `https://<DOMAIN>/login/oauth2/code/google` 추가
- Naver Developers: 콜백 URL에 `https://<DOMAIN>/login/oauth2/code/naver` 추가

## 9. 최종 확인

- `https://<DOMAIN>`으로 접속해서 브라우저 자물쇠 아이콘으로 인증서가 정상 발급됐는지 확인
- Vercel 프론트에서 Google/Naver 로그인이 정상 동작하는지
- 가족 도서관 계정을 등록해 issl.go.kr 실제 크롤링(ARM 컨테이너 안 Chromium 실동작)이 성공하는지 — **이번 배포에서 가장 위험도가 높은 지점**
- 알림 설정에서 "이 기기에서 푸시 알림 받기"를 켰을 때 브라우저 권한 프롬프트가 뜨고 구독이 VM의 `push_subscription` 테이블에 저장되는지

## 재배포 (2번째부터)

로컬에서 코드 수정 → push → VM에 SSH 접속 → `~/book-loan-status/deploy/deploy.sh` 실행이 끝.

## 알려진 제약

- 세션이 in-memory라 백엔드 재배포/재시작마다 로그인 중이던 모든 사용자가 로그아웃된다. 문제가 되면 Redis 등 영속 세션 스토어 도입을 고려할 것.
- `PushNotificationScheduler`/`LoanSyncService`에 동시 실행 방지 장치가 없다. 개인용 앱 규모에선 위험이 낮지만 계정이 늘어나면 재검토할 것.
- VM 자체(OS 보안 패치, 디스크 사용량, Docker/Postgres 업그레이드)는 관리형 서비스가 아니라 직접 관리해야 한다 — `docker image prune -f`는 `deploy.sh`가 매번 해주지만, 디스크가 가득 차지 않는지 가끔 `df -h`로 확인할 것.
