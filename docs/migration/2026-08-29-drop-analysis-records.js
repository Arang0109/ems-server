/**
 * analysis_records 드롭 (2026-08-29, 2단계).
 *
 * 선행: 2026-08-29-analysis-records-embed.js 실행 + 신버전 배포 후
 *       성적서 출력·이행 기록·실험분석 탭이 정상인지 확인.
 *   mongosh "mongodb://<host>:27017/ems" --file docs/migration/2026-08-29-drop-analysis-records.js
 *
 * 1단계와 날짜는 같지만 <b>같은 배포창에서 실행하지 않는다</b> — 접붙이기가 잘못돼도 되돌릴
 * 근거를 며칠 남겨 두려는 것이다.
 *
 * orphan_analysis_records 가 비어 있지 않으면 멈춘다. 확인 없이 지우면 실험실 입력이 사라진다.
 */

const orphanCount = db.orphan_analysis_records.countDocuments({});
if (orphanCount > 0) {
  throw new Error(
    `orphan_analysis_records 에 ${orphanCount}건이 남아 있습니다. ` +
    `내용을 확인해 처리 방침을 정하고 비운 뒤 다시 실행하세요.`);
}

const embedded = db.schedule_documents.countDocuments({ "items.analysis": { $exists: true } });
const remaining = db.analysis_records.countDocuments({});
print(`analysis 를 가진 문서 ${embedded}건 / analysis_records 잔여 ${remaining}건`);

db.analysis_records.drop();
print("완료 — analysis_records 를 드롭했습니다.");
