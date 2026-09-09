/**
 * analysis_records 를 schedule_documents.items[].analysis 로 임베드한다 (2026-08-29).
 *
 * 배포 순서: 2026-08-29-schedule-snapshot-restructure.js 다음, 신버전 배포 전.
 *   mongosh "mongodb://<host>:27017/ems" --file docs/migration/2026-08-29-analysis-records-embed.js
 *
 * 왜 옮기는가 — 판정 근거(허용기준치·산소보정)와 결과값이 서로 다른 컬렉션에 사본으로 있어
 * 한 회차 안에서 갈라질 수 있었다. 측정항목 안으로 넣으면 사본이 하나가 되고, 둘을 잇는 색인과
 * 동기화 경로가 함께 사라진다.
 *
 * <b>원본 analysis_records 는 지우지 않는다.</b> 확인 후 별도 스크립트로 드롭한다
 * (2026-08-29-drop-analysis-records.js) — 접붙이기가 잘못돼도 되돌릴 근거를 남기기 위해서다.
 *
 * 고아 레코드(대응 items[] 항목이 없는 pollutantId, pollutantId 자체가 없는 레코드)는
 * orphan_analysis_records 로 대피시키고 건수를 출력한다. 조용히 버리면 실험실 입력이 사라진
 * 사실조차 드러나지 않는다.
 *
 * 멱등하다. items[i] 에 analysis 키가 이미 있으면 건너뛴다(값이 null 인 것과 키가 없는 것을 구분).
 *
 * 2026-09-10 수정 — pollutantId 를 Map 키로 쓸 때 String() 으로 감싼다. mongosh 에서 NumberLong 은
 * 객체라 참조로 비교되어, 대응 항목이 있어도 전부 고아로 떨어졌다. 이 수정 전에 실행한 환경은
 * docs/migration/2026-09-10-analysis-embed-fix.js 로 보정한다.
 */

const orphans = [];
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
    // 이미 붙어 있으면 그대로 두되 짝을 찾았다고 표시한다 — 표시하지 않으면 재실행할 때마다
    // 이미 반영된 레코드가 고아로 다시 분류된다.
    if (Object.prototype.hasOwnProperty.call(item, "analysis")) {
      skipped++;
      matched.add(String(item.pollutantId));
      return item;
    }

    const key = String(item.pollutantId);
    const r = byPollutant.get(key);
    if (!r) return item;

    matched.add(key);
    grafted++;
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

if (orphans.length > 0) {
  // 재실행 시 같은 레코드가 두 번 쌓이지 않도록 _id 를 그대로 쓴다.
  db.orphan_analysis_records.bulkWrite(
    orphans.map(o => ({ replaceOne: { filter: { _id: o._id }, replacement: o, upsert: true } })),
    { ordered: false });
  print(`⚠ 고아 분석 레코드 ${orphans.length}건을 orphan_analysis_records 로 대피시켰습니다. ` +
        `드롭 스크립트를 실행하기 전에 내용을 확인하세요.`);
}

print(`접붙이기 완료: grafted=${grafted}, 이미 처리됨=${skipped}, 고아=${orphans.length}`);
print("원본 analysis_records 는 그대로 두었습니다 — 확인 후 2026-08-29-drop-analysis-records.js 를 실행하세요.");
