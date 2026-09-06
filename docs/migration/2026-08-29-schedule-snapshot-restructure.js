/**
 * ScheduleSnapshot 애그리거트 재구성 백필 (2026-08-29).
 *
 * 배포 순서 (규칙 15) — 이 순서를 어기면 저장이 깨집니다:
 *   1. 구버전 애플리케이션 중지            ← 구버전은 새 구조를 읽지 못합니다(하위 호환 없음)
 *   2. mongodump --db ems --collection schedule_documents --collection analysis_records
 *   3. mongosh "mongodb://<host>:27017/ems" --file docs/migration/2026-08-29-schedule-snapshot-restructure.js
 *   4. mongosh ... --file docs/migration/2026-08-29-analysis-records-embed.js
 *   5. MySQL: docs/migration/2026-08-29-tenant-report-staff.sql
 *   6. 신버전 백엔드 배포
 *   7. ems-web 신버전 배포                ← 응답 키가 바뀌므로 같은 배포창 안에서 끝냅니다
 *   8. 성적서·이행 기록 확인 후 docs/migration/2026-08-29-drop-analysis-records.js
 *
 * 선행 조건: 2026-08-18-schedule-document-version.js 가 적용되어 있어야 합니다.
 * 요구 버전: MongoDB 4.2+ (파이프라인 형태의 updateMany)
 *
 * 무엇을 하는가
 *   A. 문서 레벨 재배치 — equipments→team.equipments, sheets→samplingData.sheets,
 *      basicInfo 의 채취시각·현장담당자→samplingData.*, basicInfo·status 제거.
 *      성적서 기본정보(관리번호·측정분야·측정용도·일자 4종)와 상태는 메타(MySQL)가 진실이므로
 *      문서에서 지웁니다. items 는 루트 그대로 둡니다.
 *   B. 배열 내부 키 개명 39건 — sheets[] 와 sheets[].samplingPoints[] 안쪽.
 *      $rename 은 배열 요소 내부에 쓸 수 없으므로 문서를 읽어 치환합니다.
 *   C. 옛 패키지 경로(_class 판별자) 검출.
 *
 * 멱등하다. 옛 키가 있고 새 키가 없을 때만 옮기므로 몇 번 실행해도 결과가 같습니다.
 * createdAt·version 은 건드리지 않습니다.
 *
 * 되돌릴 수 없습니다 — status·basicInfo 삭제는 복구 경로가 없습니다(다만 메타가 진실의 원천이라
 * 손실이 아닙니다). 2번의 덤프가 유일한 복구 수단입니다.
 */

// ───────── 선행 조건 검사 ─────────
if (parseInt(db.version().split(".")[0], 10) < 4) {
  throw new Error("MongoDB 4.2 이상이 필요합니다. 현재: " + db.version());
}
const missingVersion = db.schedule_documents.countDocuments({ version: { $exists: false } });
if (missingVersion > 0) {
  throw new Error(
    `version 이 없는 문서가 ${missingVersion}건 있습니다. ` +
    `docs/migration/2026-08-18-schedule-document-version.js 를 먼저 실행하세요.`);
}

// ───────── Stage A. 문서 레벨 재배치 ─────────
const stageA = db.schedule_documents.updateMany({}, [
  {
    $set: {
      // 장비 목록을 팀 스냅샷 안으로. 이미 옮겨졌으면 그대로 둡니다.
      team: {
        $cond: [
          { $eq: [{ $type: "$team" }, "object"] },
          { $mergeObjects: [
              "$team",
              { equipments: { $ifNull: ["$team.equipments", { $ifNull: ["$equipments", []] }] } }
          ]},
          "$team"
        ]
      },
      // 그 회차의 채취 사실(시각·현장 담당자·기록지)을 한 노드로.
      samplingData: {
        $mergeObjects: [
          { $ifNull: ["$samplingData", {}] },
          {
            samplingStartedAt: { $ifNull: ["$samplingData.samplingStartedAt", "$basicInfo.samplingStartedAt"] },
            samplingEndedAt:   { $ifNull: ["$samplingData.samplingEndedAt",   "$basicInfo.samplingEndedAt"] },
            facilityManager:   { $ifNull: ["$samplingData.facilityManager",   "$basicInfo.facilityManager"] },
            samplingWitness:   { $ifNull: ["$samplingData.samplingWitness",   "$basicInfo.samplingWitness"] },
            sheets:            { $ifNull: ["$samplingData.sheets", { $ifNull: ["$sheets", []] }] }
          }
        ]
      },
      // 성적서 서명란 담당자는 고객사 스냅샷으로. 원장 기본값은 5번 SQL이 따로 채웁니다.
      tenant: {
        $cond: [
          { $eq: [{ $type: "$tenant" }, "object"] },
          { $mergeObjects: [
              "$tenant",
              { analyst:          { $ifNull: ["$tenant.analyst",          "$basicInfo.analyst"] },
                technicalManager: { $ifNull: ["$tenant.technicalManager", "$basicInfo.technicalManager"] } }
          ]},
          "$tenant"
        ]
      }
    }
  },
  // 상태와 성적서 기본정보의 진실은 MySQL schedules 하나뿐입니다. 문서의 사본을 지웁니다.
  { $unset: ["status", "equipments", "sheets", "basicInfo"] }   // createdAt 은 문서 생성 시각이므로 남긴다
]);
print(`[A] 문서 레벨 재배치: matched=${stageA.matchedCount}, modified=${stageA.modifiedCount}`);

// ───────── Stage B. 배열 내부 키 개명 (39건) ─────────

/** 옛 키가 있고 새 키가 없을 때만 옮긴다 — 이것이 멱등성의 전부다. */
function rename(obj, map) {
  if (obj === null || typeof obj !== "object") return obj;
  for (const [from, to] of Object.entries(map)) {
    if (from === to) continue;
    if (Object.prototype.hasOwnProperty.call(obj, from)) {
      if (!Object.prototype.hasOwnProperty.call(obj, to)) obj[to] = obj[from];
      delete obj[from];
    }
  }
  return obj;
}

const SHEET       = { quantity: "flowRate", particle: "particulateSampling",
                      samples: "gaseousSamplings", samplingPointCnt: "samplingPointCount" };
const WEATHER     = { pressure: "atmosphericPressure", Pa: "atmosphericPressureMmHg" };
const MOISTURE    = { weight: "bottleWeight", Pm_g: "gasMeterGaugePressureMmHg",
                      Pm_g_inch: "gasMeterGaugePressureInH2O", Tm_g: "averageGasMeterTemperature",
                      Vm_g: "sampledDryGasVolume", ma: "absorbedMoistureMass", Xw: "moistureRatio" };
const FLOW        = { avgTs: "averageGasTemperature", avgTg: "averageGasTemperatureKelvin",
                      avgPv: "averageDynamicPressure", avgPs: "averageStaticPressure",
                      area: "stackArea", Vs: "averageGasVelocity",
                      quantity: "wetGasFlowRate", standardQuantity: "standardDryGasFlowRate",
                      Cp: "appliedPitotCoefficient" };
const PARTICULATE = { avgKFactor: "averageKFactor", avgOrificeDp: "averageOrificeDifferentialPressure",
                      avgIsokineticRatio: "averageIsokineticRatio", totalVm: "totalDryGasVolume",
                      samplingStartTime: "samplingStartedAt", samplingEndTime: "samplingEndedAt" };
const GASEOUS     = { startTime: "samplingStartedAt", endTime: "samplingEndedAt" };
const POINT       = { Ts: "gasTemperature", Pv: "dynamicPressure",
                      Ps: "staticPressure", Vs: "gasVelocity", particle: "isokineticSampling" };
const ISOKINETIC  = { equipmentTemperature: "gasTemperature", equipmentVolume: "gasMeterVolume",
                      Vm: "sampledDryGasVolume", Vlc: "collectedWaterVolume",
                      nozzleSize: "nozzleDiameter", orificeDp: "orificeDifferentialPressure" };
const METER_TEMP  = { inTm: "inlet", outTm: "outlet", avgTm: "average" };
const METER_VOL   = { beforeVm: "before", afterVm: "after" };

function migrateSheet(sheet) {
  if (!sheet) return sheet;
  rename(sheet, SHEET);

  // 시트가 직접 들고 있던 가스미터 평균 절대온도는 입자상 집계로 옮긴다.
  if (Object.prototype.hasOwnProperty.call(sheet, "avgTm")) {
    if (sheet.avgTm !== null && sheet.avgTm !== undefined) {
      sheet.particulateSampling = sheet.particulateSampling || {};
      if (!Object.prototype.hasOwnProperty.call(sheet.particulateSampling, "averageGasMeterTemperature")) {
        sheet.particulateSampling.averageGasMeterTemperature = sheet.avgTm;
      }
    }
    delete sheet.avgTm;
  }

  rename(sheet.weather,             WEATHER);
  rename(sheet.moisture,            MOISTURE);
  rename(sheet.flowRate,            FLOW);
  rename(sheet.particulateSampling, PARTICULATE);

  for (const sample of sheet.gaseousSamplings || []) {
    rename(sample, GASEOUS);
  }

  for (const point of sheet.samplingPoints || []) {
    rename(point, POINT);
    const iso = point.isokineticSampling;
    if (!iso) continue;
    rename(iso, ISOKINETIC);
    rename(iso.gasTemperature, METER_TEMP);
    rename(iso.gasMeterVolume, METER_VOL);
  }
  return sheet;
}

let ops = [];
db.schedule_documents.find({ "samplingData.sheets.0": { $exists: true } }).forEach(doc => {
  ops.push({ updateOne: {
    filter: { _id: doc._id },
    update: { $set: { "samplingData.sheets": doc.samplingData.sheets.map(migrateSheet) } }
  }});
  if (ops.length === 500) { db.schedule_documents.bulkWrite(ops, { ordered: false }); ops = []; }
});
if (ops.length) db.schedule_documents.bulkWrite(ops, { ordered: false });
print("[B] 시트 배열 내부 키 개명 완료");

// ───────── Stage C. 옛 패키지 경로 방어 ─────────
// EquipmentSpec(sealed) 만 _class 를 갖지만, 다른 경로가 남아 있으면 역직렬화가 깨집니다.
const stale = db.schedule_documents.countDocuments(
  { $where: "JSON.stringify(this).indexOf('schedule.domain.sheet.') >= 0" });
print(stale > 0
  ? `[C] ⚠ 옛 패키지 경로(domain.sheet)를 참조하는 문서 ${stale}건 — 수동 확인이 필요합니다.`
  : "[C] 옛 패키지 경로 참조 없음");

// ───────── 검증 ─────────
const leftovers = db.schedule_documents.countDocuments({
  $or: [
    { status: { $exists: true } },
    { basicInfo: { $exists: true } },
    { equipments: { $exists: true } },
    { sheets: { $exists: true } },
    { "samplingData.sheets.quantity": { $exists: true } },
    { "samplingData.sheets.samples": { $exists: true } },
    { "samplingData.sheets.particle": { $exists: true } },
    { "samplingData.sheets.avgTm": { $exists: true } },
    { "samplingData.sheets.samplingPointCnt": { $exists: true } },
    { "samplingData.sheets.samplingPoints.Ts": { $exists: true } },
    { "samplingData.sheets.particulateSampling.samplingStartTime": { $exists: true } },
    { "samplingData.sheets.gaseousSamplings.startTime": { $exists: true } }
  ]
});
if (leftovers > 0) throw new Error(`변환되지 않은 문서가 ${leftovers}건 남았습니다.`);
print("완료 — 옛 구조가 남아 있지 않습니다.");
