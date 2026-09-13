-- 측정방법(method)을 pollutant_catalog(전역 가이드)에서 pollutants(고객사 채택 물질)로 이관 (2026-09-13).
--
-- 배포 순서 (규칙 15):
--   구버전 중지 → 이 스크립트 실행 → 신버전 배포 → ems-web 배포
--   mysql -u <user> -p ems < docs/migration/2026-09-13-pollutants-method.sql
--
-- 왜 옮기는가 — 같은 물질이라도 업체마다 측정방법이 다를 수 있다(예: 이황화메틸은 테드라백·카트리지
-- 둘 다 쓰인다). 가이드 하나가 전 고객사의 측정방법을 정하는 구조로는 이를 표현할 수 없어,
-- 측정방법을 고객사가 채택 시 정하는 소유값으로 바꾼다. 카탈로그에는 기본값도 남기지 않는다.
--
-- 무엇을 하는가
--   1) pollutants.method 컬럼을 보장한다. ddl-auto: update 가 신버전 기동 시 자동 추가하지만,
--      2)의 백필이 컬럼을 먼저 필요로 하므로 여기서 멱등하게 만든다.
--   2) 기존 채택 행에 카탈로그의 method 를 복사한다(채택 시점 값 = 지금까지 실제로 쓰던 값).
--      카탈로그 method 가 NULL 이던 항목은 NULL 로 남는다 — 고객사가 PUT /api/pollutants/{id} 로 채운다.
--      신규 채택(POST /api/pollutants)은 method 가 필수다.
--   3) pollutant_catalog.method 컬럼을 제거한다. ddl-auto 는 컬럼을 지우지 않으므로 수동 실행이 필요하다.
--      신버전 코드는 이 컬럼을 참조하지 않는다.
--
-- 실행 전 영향 범위를 먼저 본다.
--     SELECT c.method, COUNT(*) FROM pollutants p JOIN pollutant_catalog c ON c.catalog_id = p.catalog_id GROUP BY c.method;
--
-- 멱등하다. 컬럼이 이미 있으면 건너뛰고, 백필은 method IS NULL 인 행만 대상이며, 카탈로그 컬럼이
-- 이미 없으면 백필과 DROP 모두 아무것도 하지 않는다. 3) 이후에는 2)의 소스가 사라지므로 재실행해도
-- 값이 바뀌지 않는다.

-- 1) pollutants.method 보장 — 타입은 카탈로그 컬럼과 동일하게 복사한다(이미 없으면 varchar(255)).
--    Hibernate 가 @Enumerated(STRING) 을 VARCHAR 로 만들든 ENUM(...) 으로 만들든 같은 DB 안에서는 일관된다.
SET @method_type := (
  SELECT COALESCE(
    (SELECT COLUMN_TYPE FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pollutant_catalog' AND COLUMN_NAME = 'method'),
    'varchar(255)'));
SET @ddl := (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pollutants' AND COLUMN_NAME = 'method'),
    'SELECT "pollutants.method 이미 존재" AS msg',
    CONCAT('ALTER TABLE pollutants ADD COLUMN method ', @method_type,
           ' NULL COMMENT "측정방법(고객사 채택값, MeasurementMethod)"')));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 백필 — 카탈로그 컬럼이 아직 있을 때만. PREPARE 는 단일 문장만 받으므로 한 줄로 둔다.
SET @ddl := (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pollutant_catalog' AND COLUMN_NAME = 'method'),
    'UPDATE pollutants p JOIN pollutant_catalog c ON c.catalog_id = p.catalog_id SET p.method = c.method WHERE p.method IS NULL AND c.method IS NOT NULL',
    'SELECT "pollutant_catalog.method 없음 — 백필 생략" AS msg'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SELECT ROW_COUNT() AS backfilled_rows;

-- 3) pollutant_catalog.method 제거
SET @ddl := (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pollutant_catalog' AND COLUMN_NAME = 'method'),
    'ALTER TABLE pollutant_catalog DROP COLUMN method',
    'SELECT "pollutant_catalog.method 컬럼이 이미 없습니다" AS msg'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 확인
SELECT method, COUNT(*) AS cnt FROM pollutants GROUP BY method;
-- method 가 NULL 인 행은 고객사가 측정물질 수정 화면에서 채운다.
--
-- Hibernate 6 이 pollutant_catalog 에 CHECK (method IN (...)) 를 만들어 두었더라도 MySQL 8.0.16+ 는
-- 그 컬럼만 참조하는 CHECK 를 DROP COLUMN 과 함께 지운다. 확인만 해 둔다.
--     SHOW CREATE TABLE pollutant_catalog;
