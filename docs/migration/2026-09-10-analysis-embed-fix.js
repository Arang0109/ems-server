/**
 * analysis_records 임베드 보정 (2026-09-10).
 *
 * 선행: 2026-08-29-analysis-records-embed.js 를 이미 실행했고 grafted=0 으로 끝난 환경.
 *   mongosh "mongodb://<host>:27017/ems" --file docs/migration/2026-09-10-analysis-embed-fix.js
 *
 * 왜 필요한가 — 원 스크립트는 pollutantId 를 JS Map 의 키로 썼다. mongosh 에서 NumberLong 은
 * 객체라 Map 이 값이 아니라 참조로 비교하고, 같은 값이라도 다른 인스턴스면 get() 이 undefined 다.
 * 그래서 대응 항목이 멀쩡히 있는데도 모든 레코드가 POLLUTANT_NOT_IN_ITEMS 로 떨어졌다
 * (운영 96건 전부). 키를 문자열로 바꿔 다시 붙인다.
 *
 * 접붙인 레코드는 orphan_analysis_records 에서 지운다. 원 스크립트가 잘못 대피시킨 것이라
 * 남겨 두면 2026-08-29-drop-analysis-records.js 의 "고아가 있으면 멈춘다" 가드가 뜻을 잃는다.
 * 진짜 고아(측정항목이 교체되며 빠진 것)는 그대로 남으므로, 그 가드는 계속 유효하다.
 *
 * 멱등하다. items[i] 에 analysis 키가 이미 있으면 건너뛴다(값이 null 인 것과 키가 없는 것을 구분).
 * 원본 analysis_records 는 건드리지 않는다 — 확인 후 드롭 스크립트가 지운다.
 */

const orphans = [];
const graftedIds = [];
let grafted = 0;
let skipped = 0;
let ops = [];

db.schedule_documents.find({ "items.0": { $exists: true } }).forEach(doc => {
  const records = db.analysis_records
    .find({ scheduleId: doc.scheduleId, tenantId: doc.tenantId })
    .sort({ createdAt: 1 })
    .toArray();
  if (records.length === 0) return;

  // 등록순 정렬이므로 뒤엣것이 나중 기록이다(옛 AnalysisRecordIndexer 의 "나중 것 우선"과 같은 규칙).
  // 키는 반드시 문자열이다 — NumberLong 을 그대로 키로 쓰면 참조 비교가 되어 영영 못 찾는다.
  const byPollutant = new Map();
  for (const r of records) {
    if (r.pollutantId === null || r.pollutantId === undefined) {
      orphans.push(Object.assign({}, r, { orphanReason: "NO_POLLUTANT_ID" }));
      continue;
    }
    byPollutant.set(String(r.pollutantId), r);
  }

  const matched = new Set();
  const items = doc.items.map(item => {
    if (!item) return item;
    if (Object.prototype.hasOwnProperty.call(item, "analysis")) { skipped++; return item; }

    const key = String(item.pollutantId);
    const r = byPollutant.get(key);
    if (!r) return item;

    matched.add(key);
    grafted++;
    graftedIds.push(r._id);
    // 대리키·테넌시·판정 근거 사본은 상위 문서와 항목이 이미 갖고 있으므로 옮기지 않는다.
    return Object.assign({}, item, {
      analysis: {
        analysisValue:     r.analysisValue     === undefined ? null : r.analysisValue,
        unit:              r.unit              === undefined ? null : r.unit,
        analysisMethod:    r.analysisMethod    === undefined ? null : r.analysisMethod,
        analysisEquipment: r.analysisEquipment === undefined ? null : r.analysisEquipment,
        samplingStartedAt: r.samplingStartedAt === undefined ? null : r.samplingStartedAt,
        samplingEndedAt:   r.samplingEndedAt   === undefined ? null : r.samplingEndedAt
      }
    });
  });

  for (const [key, r] of byPollutant) {
    if (!matched.has(key)) {
      orphans.push(Object.assign({}, r, { orphanReason: "POLLUTANT_NOT_IN_ITEMS" }));
    }
  }

  ops.push({ updateOne: { filter: { _id: doc._id }, update: { $set: { items: items } } } });
  if (ops.length === 500) { db.schedule_documents.bulkWrite(ops, { ordered: false }); ops = []; }
});
if (ops.length) db.schedule_documents.bulkWrite(ops, { ordered: false });

// 접붙은 것은 고아가 아니다 — 원 스크립트가 남긴 잘못된 대피분을 걷어낸다.
if (graftedIds.length > 0) {
  const removed = db.orphan_analysis_records.deleteMany({ _id: { $in: graftedIds } });
  print(`orphan_analysis_records 에서 ${removed.deletedCount}건을 걷어냈습니다(접붙은 레코드).`);
}

if (orphans.length > 0) {
  // 재실행 시 같은 레코드가 두 번 쌓이지 않도록 _id 를 그대로 쓴다.
  db.orphan_analysis_records.bulkWrite(
    orphans.map(o => ({ replaceOne: { filter: { _id: o._id }, replacement: o, upsert: true } })),
    { ordered: false });
}

print(`보정 완료: grafted=${grafted}, 이미 처리됨=${skipped}, 남은 고아=${orphans.length}`);
print(`검증 — analysis 를 가진 문서 ${db.schedule_documents.countDocuments({ "items.analysis": { $exists: true } })}건 / ` +
      `orphan 잔여 ${db.orphan_analysis_records.countDocuments({})}건 / ` +
      `analysis_records 원본 ${db.analysis_records.countDocuments({})}건`);
print("남은 고아는 측정항목이 교체되며 빠진 기록입니다. 내용을 확인하고 비운 뒤 드롭 스크립트를 실행하세요.");
