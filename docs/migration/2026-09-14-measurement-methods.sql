-- 측정방법 승격 배포 묶음 — MySQL (2026-09-14).
--
-- 배포 순서 (규칙 15):
--   구버전 중지 → 이 스크립트 실행 → docs/migration/2026-09-14-measurement-methods.js 실행 → 신버전 배포 → ems-web 배포
--   mysql -u <user> -p ems < docs/migration/2026-09-14-measurement-methods.sql
--
-- 이 배포가 바꾸는 것 (한 묶음이라 한 파일이다)
--   A. 측정방법(MeasurementMethod)을 전역 enum에서 테넌트 소유 테이블 measurement_methods 로 승격한다.
--      카트리지·흡착관 항목은 한 번의 채취로 그 방법의 항목 전부를 함께 잡는다. 채취시간·통칭 시료명은 물질이 아니라
--      측정방법에 종속되는 값이라, pollutants 에 두면 항목마다 반복 저장하고 바꿀 때마다 동기화해야 한다(이행 종속).
--      pollutants.method(enum 문자열) → pollutants.method_id(FK).
--   B. pollutants.sampling_minutes — 항목별 채취시간 오버라이드. 흡수액처럼 항목마다 따로 잡는 방법은 물질마다
--      흡인 시간이 다를 수 있어 방법 기본값을 덮어쓴다. 컬럼 추가만 있고 백필은 없다(전부 NULL = 방법 기본값).
--   C. pollutant_catalog.mode — 측정방식 분류(현장측정·먼지·중금속·수은·가스상 채취). 회사 측정방법이 아무리
--      쪼개져도 전 tenant 를 관통해 항목을 묶는 축이다. 시드(PollutantCatalogInitializer)는 없는 code 만 추가하고
--      기존 행은 건드리지 않으므로 이미 시드된 48건은 여기서 채운다.
--   D. 비소화합물(AS)의 phase 를 GAS → PARTICLE 로 되돌린다. 예전 프론트가 가스상 표 행을 phase 게이트로 걸어
--      거짓값을 두었으나, 비소는 중금속 여지로 잡는 입자상 물질이다(흡수액 병행은 고객사 측정방법이 표현한다).
--
-- 실행 전 영향 범위를 먼저 본다.
--     SELECT method, COUNT(*) FROM pollutants GROUP BY method;
--     SELECT field, code, phase, mode FROM pollutant_catalog WHERE code = 'AS';
--
-- 멱등하다. 테이블·컬럼·FK 는 있으면 건너뛰고, 프리셋은 (tenant_id, name)이 없는 것만 넣으며, 백필은 NULL 인 행만
-- 대상이다. A-5) 이후에는 A-4)의 소스가 사라지므로 재실행해도 값이 바뀌지 않는다.
--
-- 컬럼·제약 이름은 엔티티(MeasurementMethodEntity·PollutantEntity·PollutantCatalogEntity)와 같아야 한다 —
-- 다르면 신버전 기동 시 ddl-auto: update 가 같은 것을 한 번 더 만든다.

-- ============================================================================
-- A. measurement_methods 승격
-- ============================================================================

-- A-1) measurement_methods 보장
CREATE TABLE IF NOT EXISTS measurement_methods (
  method_id          BIGINT       NOT NULL AUTO_INCREMENT,
  tenant_id          BIGINT       NOT NULL,
  name               VARCHAR(255) NOT NULL,
  sample_grouping    VARCHAR(255) NOT NULL COMMENT '채취 단위(SampleGrouping): NONE / PER_ITEM / MERGED',
  merged_sample_name VARCHAR(255) NULL     COMMENT 'MERGED 일 때 기록지 통칭 시료명(VOCs 등)',
  sampling_minutes   INT          NULL     COMMENT '표준(계획) 채취시간, 분 — 이 방법의 기본값',
  sort_order         INT          NULL,
  created_at         DATETIME(6)  NULL,
  modified_at        DATETIME(6)  NULL,
  PRIMARY KEY (method_id),
  UNIQUE KEY uk_measurement_methods_tenant_name (tenant_id, name),
  KEY idx_measurement_methods_tenant_id (tenant_id),
  CONSTRAINT fk_measurement_methods_tenants FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- A-2) 기본 8종 프리셋을 전 tenant 에 — MeasurementMethodPreset 과 동일해야 한다. 채취시간은 기본값이 없다(업체마다 다르다).
INSERT INTO measurement_methods (tenant_id, name, sample_grouping, merged_sample_name, sampling_minutes, sort_order, created_at, modified_at)
SELECT t.tenant_id, p.name, p.sample_grouping, p.merged_sample_name, NULL, p.sort_order, NOW(6), NOW(6)
FROM tenants t
CROSS JOIN (
  SELECT '먼지'     AS name, 'NONE'     AS sample_grouping, NULL     AS merged_sample_name, 10 AS sort_order UNION ALL
  SELECT '중금속',           'NONE',                        NULL,                          20 UNION ALL
  SELECT '수은',             'NONE',                        NULL,                          30 UNION ALL
  SELECT '현장측정',         'NONE',                        NULL,                          40 UNION ALL
  SELECT '흡수액',           'PER_ITEM',                    NULL,                          50 UNION ALL
  SELECT '흡착관',           'MERGED',                      'VOCs-T',                      60 UNION ALL
  SELECT '테드라백',         'PER_ITEM',                    NULL,                          70 UNION ALL
  SELECT '카트리지',         'MERGED',                      'VOCs',                        80
) p
WHERE NOT EXISTS (
  SELECT 1 FROM measurement_methods m WHERE m.tenant_id = t.tenant_id AND m.name = p.name
);
SELECT ROW_COUNT() AS inserted_presets;

-- A-3) pollutants.method_id 컬럼·FK 보장. 백필되지 못한 레거시 행 호환을 위해 NULL 허용이다.
SET @ddl := (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pollutants' AND COLUMN_NAME = 'method_id'),
    'SELECT "pollutants.method_id 이미 존재" AS msg',
    'ALTER TABLE pollutants ADD COLUMN method_id BIGINT NULL COMMENT "측정방법(measurement_methods FK)"'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pollutants' AND CONSTRAINT_NAME = 'fk_pollutants_measurement_methods'),
    'SELECT "fk_pollutants_measurement_methods 이미 존재" AS msg',
    'ALTER TABLE pollutants ADD CONSTRAINT fk_pollutants_measurement_methods FOREIGN KEY (method_id) REFERENCES measurement_methods (method_id)'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- A-4) 백필 — 구 enum 컬럼이 아직 있을 때만. CASE 의 이름은 A-2)의 프리셋 이름과 같아야 한다.
--      PREPARE 는 단일 문장만 받으므로 한 줄로 둔다.
SET @ddl := (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pollutants' AND COLUMN_NAME = 'method'),
    'UPDATE pollutants p JOIN measurement_methods m ON m.tenant_id = p.tenant_id AND m.name = CASE p.method WHEN ''DUST'' THEN ''먼지'' WHEN ''HEAVY_METAL'' THEN ''중금속'' WHEN ''MERCURY'' THEN ''수은'' WHEN ''FIELD_MEASUREMENT'' THEN ''현장측정'' WHEN ''ABSORPTION_SOLUTION'' THEN ''흡수액'' WHEN ''ADSORPTION_TUBE'' THEN ''흡착관'' WHEN ''TEDLAR_BAG'' THEN ''테드라백'' WHEN ''CARTRIDGE'' THEN ''카트리지'' END SET p.method_id = m.method_id WHERE p.method_id IS NULL AND p.method IS NOT NULL',
    'SELECT "pollutants.method 없음 — 백필 생략" AS msg'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SELECT ROW_COUNT() AS backfilled_method_rows;

-- A-5) pollutants.method 제거 — 매핑되지 못한 행이 남아 있으면 지우지 않는다(값이 유실된다).
--      남은 행은 아래 확인 쿼리로 보고, 프리셋에 없는 값이면 A-2)에 이름을 추가해 재실행한다.
SET @has_legacy := EXISTS(SELECT 1 FROM information_schema.COLUMNS
                          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pollutants' AND COLUMN_NAME = 'method');
SET @ddl := (
  SELECT IF(NOT @has_legacy,
    'SELECT "pollutants.method 컬럼이 이미 없습니다" AS msg',
    'SELECT COUNT(*) INTO @unmapped FROM pollutants WHERE method IS NOT NULL AND method_id IS NULL'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl := (
  SELECT IF(NOT @has_legacy,
    'SELECT "skip" AS msg',
    IF(@unmapped = 0,
      'ALTER TABLE pollutants DROP COLUMN method',
      CONCAT('SELECT "백필되지 못한 행 ', @unmapped, '건 — pollutants.method 를 유지합니다" AS msg'))));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================================
-- B. pollutants.sampling_minutes — 항목별 채취시간 오버라이드 (백필 없음)
-- ============================================================================
SET @ddl := (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pollutants' AND COLUMN_NAME = 'sampling_minutes'),
    'SELECT "pollutants.sampling_minutes 이미 존재" AS msg',
    'ALTER TABLE pollutants ADD COLUMN sampling_minutes INT NULL COMMENT "항목별 채취시간 오버라이드(분). NULL 이면 측정방법 기본값"'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================================
-- C. pollutant_catalog.mode — 측정방식 분류
-- ============================================================================

-- C-1) 컬럼 보장
SET @ddl := (
  SELECT IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pollutant_catalog' AND COLUMN_NAME = 'mode'),
    'SELECT "pollutant_catalog.mode 이미 존재" AS msg',
    'ALTER TABLE pollutant_catalog ADD COLUMN mode VARCHAR(255) NULL COMMENT "측정방식 분류(MeasurementMode)"'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- C-2) 백필 — 대기(AIR) 시드 48건. 매핑은 src/main/resources/catalog/pollutant-catalog.json 과 같아야 한다.
--      비소화합물(AS)은 중금속이면서 흡수액도 하지만 주 방식 HEAVY_METAL 로 둔다. 가스상 채취의 매체는 분류가 아니다.
UPDATE pollutant_catalog
SET mode = CASE
  WHEN code IN ('TSP', 'SMOKE', 'PM10', 'PM25') THEN 'DUST'
  WHEN code IN ('NOX', 'SOX', 'CO', 'THC') THEN 'DIRECT_READING'
  WHEN code IN ('BE', 'CD', 'CR', 'CU', 'NI', 'PB', 'ZN', 'AS') THEN 'HEAVY_METAL'
  WHEN code = 'HG' THEN 'MERCURY'
  ELSE 'GAS_SAMPLING'
END
WHERE field = 'AIR' AND mode IS NULL;
SELECT ROW_COUNT() AS backfilled_mode_rows;

-- ============================================================================
-- D. 비소화합물 phase 정정
-- ============================================================================
UPDATE pollutant_catalog SET phase = 'PARTICLE' WHERE field = 'AIR' AND code = 'AS' AND phase = 'GAS';
SELECT ROW_COUNT() AS arsenic_phase_fixed;

-- ============================================================================
-- 확인
-- ============================================================================
SELECT tenant_id, COUNT(*) AS preset_count FROM measurement_methods GROUP BY tenant_id;   -- tenant 마다 8
SELECT COUNT(*) AS pollutants_without_method FROM pollutants WHERE method_id IS NULL;     -- 고객사가 PUT /api/pollutants/{id} 로 채운다
SELECT field, mode, COUNT(*) FROM pollutant_catalog GROUP BY field, mode;                 -- AIR 는 mode NULL 이 없어야 한다
SELECT code, phase, mode FROM pollutant_catalog WHERE code = 'AS';                        -- PARTICLE / HEAVY_METAL
--
-- Hibernate 6 이 pollutants 에 CHECK (method IN (...)) 를 만들어 두었더라도 MySQL 8.0.16+ 는
-- 그 컬럼만 참조하는 CHECK 를 DROP COLUMN 과 함께 지운다. 확인만 해 둔다.
--     SHOW CREATE TABLE pollutants;
