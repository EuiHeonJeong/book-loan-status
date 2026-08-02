-- public 스키마 대신 앱 전용 스키마를 쓰기로 함. 스키마명을 접속 유저(app)와 똑같이 두면
-- Postgres 기본 search_path("$user", public) 덕분에 별도 설정 없이 자동으로 이 스키마가 먼저 잡힌다
-- (이 DB의 app 롤은 search_path를 따로 오버라이드하지 않은 factory 기본값임을 확인했음).
-- flyway_schema_history는 이번엔 건드리지 않는다 — Flyway 자체 부트스트랩과 얽혀 있어
-- 별도 마이그레이션(V6)에서 안전하게 옮긴다.
CREATE SCHEMA IF NOT EXISTS app;

ALTER TABLE public.app_user SET SCHEMA app;
ALTER TABLE public.family_member SET SCHEMA app;
ALTER TABLE public.library_account SET SCHEMA app;
ALTER TABLE public.loan_record SET SCHEMA app;
ALTER TABLE public.notification_setting SET SCHEMA app;
ALTER TABLE public.push_subscription SET SCHEMA app;
