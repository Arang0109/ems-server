-- documents.category 의 'REPORT_TEMPLATE' 행 재분류 (2026-09-09).
--
-- 배포 순서 (규칙 15):
--   구버전 중지 → 이 스크립트 실행 → 신버전 배포
--   mysql -u <user> -p ems < docs/migration/2026-09-09-documents-drop-report-template.sql
--
-- 무엇을 하는가 — DocumentCategory 에서 REPORT_TEMPLATE 상수를 없앴으므로, 그 값으로 저장된
-- 기존 행을 남은 분류(ETC)로 옮긴다.
--
-- 왜 필요한가 — documents.category 는 @Enumerated(EnumType.STRING) 이라 DB 에 문자열
-- 'REPORT_TEMPLATE' 로 들어 있다. 신버전에는 그 상수가 없어 Hibernate 가 역변환에 실패하고,
-- 문제는 그 행 하나가 아니라 문서 목록 조회가 통째로 500 으로 죽는다는 점이다.
-- 코드에는 REPORT_TEMPLATE 참조가 하나도 남지 않아 컴파일·테스트로는 드러나지 않는다.
--
-- 실행 전 영향 범위를 먼저 본다. 0 건이면 이 스크립트는 기록으로만 남는다.
--
--     SELECT category, COUNT(*) FROM documents GROUP BY category;
--     SELECT document_id, tenant_id, name FROM documents WHERE category = 'REPORT_TEMPLATE';
--
-- 옮길 분류는 ETC 로 잡았다. 실제 내용이 채취기록부 양식이라면 아래 값을
-- SAMPLING_RECORD_TEMPLATE 으로 바꿔 실행한다. 남은 분류는 SAMPLING_RECORD_TEMPLATE ·
-- CONTRACT · CERTIFICATE · ETC 네 가지다.
--
-- 멱등하다. 대상 행이 없으면 0 건이 갱신될 뿐이다. 되돌리려면 옮긴 document_id 를 알아야 하므로
-- 위 SELECT 결과를 실행 전에 남겨 둔다.

UPDATE documents SET category = 'ETC' WHERE category = 'REPORT_TEMPLATE';
SELECT ROW_COUNT() AS 재분류된_행수;

-- 남은 값 확인
SELECT category, COUNT(*) AS cnt FROM documents GROUP BY category;

-- Hibernate 6 이 enum 컬럼에 check (category in (...)) 제약을 만들어 두었다면 REPORT_TEMPLATE 이
-- 목록에 남아 있을 수 있다. 값이 줄어드는 방향이라 INSERT 를 막지는 않으므로 정리는 선택이다.
-- 확인만 해 둔다.
--
--     SHOW CREATE TABLE documents;
