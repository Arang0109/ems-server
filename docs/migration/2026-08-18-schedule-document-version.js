/**
 * 측정 시트 동시 편집(낙관적 락) 도입에 따른 백필.
 *
 * 배포 전에 반드시 한 번 실행한다:
 *   mongosh "mongodb://<host>:27017/ems" --file docs/migration/2026-08-18-schedule-document-version.js
 *
 * 왜 필요한가
 *   ScheduleDocument 에 @Version 을 붙이면 Spring Data 는 version 이 null 인 문서를 **신규**로 판정해
 *   insert 를 시도한다. 기존 문서는 이 필드가 없어 null 로 읽히므로 같은 _id 로 insert 가 나가고
 *   E11000 duplicate key 로 저장이 전부 실패한다.
 *
 *   version 을 0 으로 채워 두면 신규 판정을 피한다 — 필드 타입이 래퍼(Long)라 0 은 신규로 보지 않는다
 *   (primitive long 이었다면 0 도 신규로 간주되어 이 백필이 통하지 않는다).
 *
 * 멱등하다. 이미 version 이 있는 문서는 건드리지 않으므로 여러 번 실행해도 안전하다.
 */
const result = db.schedule_documents.updateMany(
  { version: { $exists: false } },
  { $set: { version: NumberLong("0") } },
);

print(`schedule_documents version 백필: matched=${result.matchedCount}, modified=${result.modifiedCount}`);

const remaining = db.schedule_documents.countDocuments({ version: { $exists: false } });
if (remaining > 0) {
  throw new Error(`백필되지 않은 문서가 ${remaining}건 남았습니다. 저장이 실패할 수 있습니다.`);
}
print("완료 — version 이 없는 문서가 남아 있지 않습니다.");
