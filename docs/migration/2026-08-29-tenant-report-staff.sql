-- 고객사(테넌트) 원장에 성적서 서명란 담당자 2필드 추가 (2026-08-29).
--
-- 배포 순서 (규칙 15):
--   구버전 중지 → Mongo 스크립트 2개 실행 → 이 스크립트 실행 → 신버전 배포 → ems-web 배포
--   mysql -u <user> -p ems < docs/migration/2026-08-29-tenant-report-staff.sql
--
-- 왜 원장에 두는가 — 시료분석검사자·기술책임자는 측정대행업체 소속 인력이라 회사 단위 기본값이
-- 있다. 회차마다 다를 수 있으므로 측정계획 스냅샷(schedule_documents.tenant)에서 덮어쓸 수 있게
-- 두며, 이는 측정자(사수·부사수) 이름과 같은 규약이다.
--
-- ddl-auto: update 가 컬럼 추가는 자동 반영하므로 이 스크립트 없이도 애플리케이션은 뜬다.
-- 그럼에도 남기는 이유는 배포 순서를 기록하고, 기존 문서의 담당자를 원장 기본값으로 끌어올리는
-- 백필 지점을 한곳에 두기 위해서다.
--
-- 멱등하다. 이미 있으면 건너뛴다.

SET @ddl := (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tenants' AND COLUMN_NAME = 'analyst'),
    'SELECT "tenants.analyst 이미 존재" AS msg',
    'ALTER TABLE tenants ADD COLUMN analyst VARCHAR(255) NULL COMMENT "시료분석검사자(성적서 서명란 기본값)"'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tenants' AND COLUMN_NAME = 'technical_manager'),
    'SELECT "tenants.technical_manager 이미 존재" AS msg',
    'ALTER TABLE tenants ADD COLUMN technical_manager VARCHAR(255) NULL COMMENT "기술책임자(성적서 서명란 기본값)"'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 값 채우기는 운영자가 /api/platform/tenants 로 하거나 아래처럼 직접 넣는다.
-- UPDATE tenants SET analyst = '홍길동', technical_manager = '김철수' WHERE tenant_id = 1;
