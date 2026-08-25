-- 측정계획 상태 변경 이력 제거 + soft delete → 물리 삭제 전환 (2026-08-25)
--
-- ddl-auto=update 는 컬럼·제약·테이블 삭제를 반영하지 않으므로 배포 전에 직접 실행합니다.
-- 유니크 제약에서 deleted_at 을 빼기 때문에, 남아 있는 soft delete 행을 먼저 지워야
-- 같은 (tenant_id, stack_id, team_id, sampled_at) 조합이 중복돼 제약 생성이 실패하지 않습니다.
--
-- 실행 순서: 구버전 중지 → 이 스크립트 → 신버전 배포
--   mysql -u <user> -p ems < docs/migration/2026-08-25-schedule-drop-status-log.sql

-- 1) 지워진 것으로 표시된 계획을 실제로 제거한다(복구 경로가 사라졌으므로 되살릴 수 없다).
--    지우기 전에 남는 건수를 확인하려면 아래 SELECT 를 먼저 실행하세요.
-- SELECT schedule_id, tenant_id, stack_id, team_id, sampled_at, status, deleted_at
--   FROM schedules WHERE deleted_at IS NOT NULL;
DELETE FROM schedules WHERE deleted_at IS NOT NULL;

-- 2) 유니크 제약을 deleted_at 없는 형태로 다시 만든다.
ALTER TABLE schedules DROP INDEX uk_schedules_stack_team_date;
ALTER TABLE schedules
  ADD CONSTRAINT uk_schedules_stack_team_date
  UNIQUE (tenant_id, stack_id, team_id, sampled_at);

-- 3) soft delete 컬럼 제거.
ALTER TABLE schedules DROP COLUMN deleted_at;
ALTER TABLE schedules DROP COLUMN deleted_by;

-- 4) 상태 변경 이력 테이블 제거.
DROP TABLE IF EXISTS schedule_status_logs;

-- 5) 1)에서 지운 계획의 MongoDB 문서는 고아로 남습니다. 목록·상세는 MySQL 메타를 기준으로
--    조립하므로 노출되지는 않지만, 정리하려면 mongosh 에서 아래를 실행하세요.
--    (schedules 를 지우기 전에 대상 scheduleId 를 확보해 두어야 합니다.)
--
--    db.schedule_documents.deleteMany({ scheduleId: { $in: [ /* 지운 scheduleId 목록 */ ] } })
--    db.analysis_records.deleteMany({ scheduleId: { $in: [ /* 지운 scheduleId 목록 */ ] } })
