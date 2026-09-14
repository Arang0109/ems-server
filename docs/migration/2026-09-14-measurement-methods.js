/**
 * 측정방법 승격 배포 묶음 — MongoDB (2026-09-14).
 *
 * 선행: docs/migration/2026-09-14-measurement-methods.sql 을 먼저 실행한다(순서 자체는 무관하지만 배포 묶음이 같다).
 *   mongosh "mongodb://<host>:27017/ems" --file docs/migration/2026-09-14-measurement-methods.js
 *
 * 왜 필요한가 — 측정방법이 전역 enum 에서 tenant 소유 애그리거트로 승격되면서 측정계획 문서의
 * items[].method 가 "CARTRIDGE" 같은 문자열에서 사본 객체
 * {methodId, name, sampleGrouping, mergedSampleName, samplingMinutes} 로 바뀌었다. ScheduleDocument 는
 * 도메인 record 를 그대로 저장하므로, 변환하지 않으면 신버전이 기존 문서를 역직렬화하지 못한다.
 * **신버전 배포 전에 반드시 실행한다.**
 *
 * 같은 배포에서 items[] 에 두 필드가 더 생겼다. 둘 다 없으면 null 로 읽히므로 변환하지 않아도 깨지지 않지만,
 * 문서 모양을 한 가지로 맞추려고 없는 항목에 명시적으로 null 을 둔다.
 *   - samplingMinutes : 이 항목에 적용된 표준 채취시간(방법 기본값 + 항목 오버라이드). 당시엔 값 자체가 없었다.
 *   - mode            : 측정방식 분류(카탈로그 전역 사실). 과거 회차에 소급하지 않는다 — 사본 원칙.
 *
 * method 사본의 methodId 는 null 로 둔다 — mongosh 에서 MySQL 을 조인할 수 없고, 스냅샷은 사본이라 원장 연결키가
 * 없어도 성적서·기록지에 영향이 없다. 프론트는 methodId ?? name 으로 그룹을 식별한다.
 * 매핑은 MeasurementMethodPreset 과 같아야 한다. 프리셋에 없는 문자열이 나오면 이름만 그대로 옮기고
 * sampleGrouping 은 NONE 으로 두며 unknown 에 세어 마지막에 출력한다.
 *
 * 멱등하다. typeof item.method === "string" 인 원소만 바꾸고, 이미 객체이거나 null 인 것은 그대로 둔다.
 * samplingMinutes·mode 는 키가 없을 때만 null 을 넣는다.
 */

const PRESET = {
  DUST:                { name: "먼지",     sampleGrouping: "NONE",     mergedSampleName: null },
  HEAVY_METAL:         { name: "중금속",   sampleGrouping: "NONE",     mergedSampleName: null },
  MERCURY:             { name: "수은",     sampleGrouping: "NONE",     mergedSampleName: null },
  FIELD_MEASUREMENT:   { name: "현장측정", sampleGrouping: "NONE",     mergedSampleName: null },
  ABSORPTION_SOLUTION: { name: "흡수액",   sampleGrouping: "PER_ITEM", mergedSampleName: null },
  ADSORPTION_TUBE:     { name: "흡착관",   sampleGrouping: "MERGED",   mergedSampleName: "VOCs-T" },
  TEDLAR_BAG:          { name: "테드라백", sampleGrouping: "PER_ITEM", mergedSampleName: null },
  CARTRIDGE:           { name: "카트리지", sampleGrouping: "MERGED",   mergedSampleName: "VOCs" }
};

let converted = 0;
let padded = 0;
let unknown = 0;
let ops = [];

function toSnapshot(legacy) {
  const preset = PRESET[legacy];
  if (!preset) {
    unknown++;
    print(`알 수 없는 측정방법 문자열: ${legacy} — 이름만 옮기고 NONE 으로 둔다`);
    return { methodId: null, name: legacy, sampleGrouping: "NONE", mergedSampleName: null, samplingMinutes: null };
  }
  return {
    methodId: null,
    name: preset.name,
    sampleGrouping: preset.sampleGrouping,
    mergedSampleName: preset.mergedSampleName,
    samplingMinutes: null
  };
}

const hasKey = (obj, key) => Object.prototype.hasOwnProperty.call(obj, key);

db.schedule_documents.find({ "items.0": { $exists: true } }).forEach(doc => {
  let touched = false;

  const items = doc.items.map(item => {
    if (!item) return item;
    let next = item;

    if (typeof item.method === "string") {
      next = Object.assign({}, next, { method: toSnapshot(item.method) });
      converted++;
      touched = true;
    }
    if (!hasKey(next, "samplingMinutes") || !hasKey(next, "mode")) {
      next = Object.assign(
        { samplingMinutes: null, mode: null },   // 없는 키만 채운다 — 뒤의 spread 가 있는 값을 덮어쓴다
        next
      );
      padded++;
      touched = true;
    }
    return next;
  });

  if (!touched) return;

  ops.push({ updateOne: { filter: { _id: doc._id }, update: { $set: { items: items } } } });
  if (ops.length >= 500) {
    db.schedule_documents.bulkWrite(ops, { ordered: false });
    ops = [];
  }
});

if (ops.length > 0) {
  db.schedule_documents.bulkWrite(ops, { ordered: false });
}

print(`converted=${converted} padded=${padded} unknown=${unknown}`);
print(`remaining string methods: ${db.schedule_documents.countDocuments({ "items.method": { $type: "string" } })}`);
print(`items without samplingMinutes key: ${db.schedule_documents.countDocuments({ items: { $elemMatch: { samplingMinutes: { $exists: false } } } })}`);
