-- document_versions.provider 컬럼 제거 (2026-09-06)
--
-- 문서 보관소에서 라우팅 계층(RoutingFileStorageAdapter)을 걷어내면서 이 컬럼을 읽는 코드가 없어졌다.
-- 이제 보관소는 환경마다 하나(ems.storage.provider 로 LocalFileStorageAdapter 또는 S3FileStorageAdapter)이며,
-- 파일이 어디에 저장됐는지를 행마다 기록하지 않는다.
--
-- ddl-auto: update 는 컬럼을 지우지 않으므로 수동 실행이 필요하다.
-- 남겨 두면 NOT NULL + 기본값 없음이라 신버전의 INSERT 가 전부 실패한다. 반드시 배포 전에 실행한다.
--
-- 실행 순서:
--   1) 구버전 중지
--   2) 이 스크립트 실행
--        mysql -u <user> -p ems < docs/migration/2026-09-06-storage-drop-provider.sql
--   3) 신버전 배포
--
-- ⚠️ 전제: 과거에 다른 보관소로 올린 파일을 더 이상 읽지 않는다.
--    provider 가 'LOCAL' 인 행과 'S3' 인 행이 섞여 있었다면, 컬럼을 지운 뒤로는 모든 행이
--    "현재 설정된 보관소에 있다"고 가정된다. 섞여 있는 개발 DB라면 실물 파일을 한쪽으로 옮기거나
--    문서를 정리한 뒤 실행한다. 아래 SELECT 로 먼저 확인할 수 있다.
--
--        SELECT provider, COUNT(*) FROM document_versions GROUP BY provider;
--
-- 멱등하다. 컬럼이 이미 없으면 아무 것도 하지 않는다.

SET @ddl = (
  SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE document_versions DROP COLUMN provider',
    'SELECT "document_versions.provider 컬럼이 이미 없습니다" AS result'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'document_versions'
    AND column_name = 'provider'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
