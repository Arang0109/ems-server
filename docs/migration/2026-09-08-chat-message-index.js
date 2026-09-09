// chat_messages 복합 인덱스 보정 (멱등)
//
// spring.data.mongodb.auto-index-creation=true 가 @CompoundIndex 를 만들지만, 그 생성은
// 컬렉션 첫 접근 시점에 일어나고 실패해도 조용하다. 대화 이력 페이징과 미읽음 집계가 전부
// 이 인덱스에 기대고 있어, 없으면 컬렉션 풀스캔이 되고도 기능은 정상 동작하는 것처럼 보인다.
// 배포 후 명시적으로 확인·보정한다.
//
// 배포 순서: 신버전 배포 → 이 스크립트 실행 → db.chat_messages.getIndexes() 로 확인
//
// 실행: mongosh "$MONGO_URI" --file docs/migration/2026-09-08-chat-message-index.js

db.chat_messages.createIndex(
  { tenantId: 1, roomId: 1, _id: -1 },
  { name: "idx_chat_messages_tenant_room_id", background: true }
);

// tenantId 단일 인덱스(@Indexed). 위 복합 인덱스의 접두사와 겹치지만 Spring Data 가 선언대로 만들므로
// 실제 상태와 코드가 어긋나지 않도록 함께 보정한다.
db.chat_messages.createIndex({ tenantId: 1 }, { name: "tenantId", background: true });

printjson(db.chat_messages.getIndexes());
