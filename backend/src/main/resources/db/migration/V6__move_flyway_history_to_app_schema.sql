-- V5에서 business 테이블은 app 스키마로 옮겼는데, Flyway 자체 부트스트랩과 얽히는 걸 피하려고
-- flyway_schema_history는 그때 남겨뒀다. 이제 별도 마이그레이션으로 안전하게 옮긴다.
-- 이후로는 public 스키마에 아무 것도 남지 않는다 — search_path("$user", public)가 app 롤과
-- 이름이 같은 app 스키마를 항상 먼저 찾으므로, 이 이후의 마이그레이션/Flyway 자신도
-- 별도 설정 없이 계속 app 스키마를 쓴다.
--
-- IF EXISTS로 감싸는 이유: app 스키마가 생기고 나면 Flyway는 다음 부팅 때부터 이력 테이블을
-- app 스키마에서 먼저 찾는다(아직 public에 있어도). 그 상태에서 재부팅하면 "app 스키마에
-- 데이터는 있는데 이력 테이블이 없다"며 Flyway가 아예 기동을 거부해 이 마이그레이션을 실행할
-- 기회조차 못 얻는 순환 문제가 생긴다 — 로컬에서 실제로 겪었다. 그래서 이미 수동으로 옮겨진
-- 환경(이 프로젝트의 기존 로컬 DB)에서도, 아직 안 옮겨진 새 환경(예: 배포 시 빈 DB에서
-- V1부터 순서대로 실행되는 경우)에서도 이 스크립트가 안전하게 동작하도록 조건부로 처리한다.
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'flyway_schema_history'
  ) THEN
    ALTER TABLE public.flyway_schema_history SET SCHEMA app;
  END IF;
END $$;
