# chat 모듈 가이드라인

같은 고객사 구성원끼리 나누는 **1:1 대화**를 담당하는 모듈입니다. 방과 참가자는 MySQL 원장이고,
메시지는 MongoDB에 쌓이며, 접속 상태는 Redis에 있습니다. 실시간 전달은 STOMP over WebSocket이고
**쓰기는 전부 REST**입니다.

---

## 애그리거트

| 애그리거트 | 저장소 | 도메인 루트 | 비고 |
|---|---|---|---|
| **ChatRoom** | MySQL `chat_rooms` | `domain/ChatRoom` | tenant 종속. `tenant_id`는 plain `Long` 컬럼 |
| **ChatParticipant** | MySQL `chat_room_participants` | `domain/ChatParticipant` | 방당 2건. 읽음 커서와 `hidden`을 보유 |
| **ChatMessage** | MongoDB `chat_messages` | `domain/ChatMessage` | append-only. 첨부를 임베드 |
| **Presence** | Redis `PRESENCE:*` | `domain/PresenceStatus` | 원장이 아니라 휘발성 상태 |

```
ChatRoom (대화방)              MySQL   ── lastMessageId ──▶ chat_messages (요약 사본)
  └── ChatParticipant  1:2     MySQL   ── lastReadMessageId ──▶ 미읽음 기준점
ChatMessage                    Mongo   ── attachment.storageKey ──▶ 파일 실물 (global/storage)
```

**`ChatParticipant`는 `ChatRoom`과 JPA 연관을 두지 않습니다.** 같은 모듈이라 걸어도 규칙 위반은
아니지만, 조회 방향이 언제나 **참가자 → 방**("내 대화방 목록")이라 방에서 참가자를 객체 그래프로
끌 일이 없습니다. `ScheduleEntity`가 연관관계를 하나도 두지 않은 것과 같은 판단입니다.

---

## 이 모듈의 핵심 결정 4가지

### 1. "두 사람당 방 하나"는 DB 제약이 지킵니다

`pair_key = min(userId):max(userId)` + `UNIQUE (tenant_id, pair_key)`.

**애플리케이션의 존재 검사로는 지킬 수 없습니다** — 두 사람이 동시에 대화를 시작하면 둘 다
"없다"를 읽고 둘 다 만듭니다. 유일성을 보장하는 것은 제약뿐이고, 그 충돌은 **예외가 아니라 신호**로
다룹니다("다른 요청이 방금 만들었다" → 재조회해서 그 방을 반환).

그 정책은 `application/service/support/DirectRoomWriter`에 있으며 `@Transactional(REQUIRES_NEW)`입니다.
호출자의 트랜잭션 안에서 유니크 충돌이 나면 그 트랜잭션이 rollback-only가 되어 예외를 잡아 복구해도
커밋 시점에 통째로 죽기 때문입니다.

> `ChatRoomRepositoryAdapter.save`가 `saveAndFlush`인 것도 이 때문입니다. flush 하지 않으면 제약 위반이
> 커밋까지 미뤄져 `DirectRoomWriter`의 try 밖에서 터집니다.

### 2. 미읽음은 저장하지 않고 셉니다

MySQL 카운터 증가와 Mongo insert는 2PC를 걸 수 없어 **반드시 어긋납니다.** 파생값을 두지 않고
`lastReadMessageId` 뒤를 매번 셉니다.

방마다 count를 날리면 목록 조회가 N+1이므로, 방별 커서를 한꺼번에 넘겨 **집계 한 번**으로 받습니다
(`ChatMessageRepository.countUnreadByRoom`). 이 하나만 `MongoTemplate`을 직접 씁니다 — "방마다 커서가
다르다"는 `$or` 조건을 파생 메서드로 표현할 수 없습니다.

### 3. 읽음 커서는 앞으로만 갑니다

위로 스크롤해 옛 메시지를 보는 것은 읽음 취소가 아니고, 여러 탭의 보고가 뒤바뀐 순서로 도착할 수도
있습니다. 되돌아가면 **이미 읽은 메시지가 다시 안 읽음으로 살아납니다** — 사용자 눈에는 유령 알림입니다.

비교는 Mongo `ObjectId` 문자열의 사전순입니다. 앞 4바이트가 초 단위 타임스탬프이고 뒤이어 단조 증가
카운터가 오므로 16진 문자열의 사전순이 곧 생성순입니다. **커서 페이징도 같은 성질에 기댑니다.**

> `sentAt`을 커서로 쓰지 않는 이유: 같은 밀리초에 두 건이 들어올 수 있어 경계에서 메시지가
> 누락되거나 중복됩니다.

### 4. 접속 상태는 컬럼이 아니라 TTL 키입니다

`users`에 컬럼을 두면 연결·해제마다 UPDATE가 나가고, 프로세스가 `kill -9`로 죽으면 그 순간 접속해
있던 사용자가 **영원히 온라인**으로 박힙니다. 진실의 원천은 살아 있는 WebSocket 세션이며,
Redis TTL(90초)이 그 상태를 스스로 청소합니다. 살아 있는 세션은 30초 주기 갱신이 만료를 미룹니다.

키의 값이 **세션 id 집합**인 이유는 다중 탭입니다. 사용자 단위 플래그였다면 탭 두 개 중 하나만
닫아도 오프라인이 됩니다.

---

## 두 저장소에 걸친 쓰기 순서

메시지 전송(`ChatMessageService.sendMessage`):

```
① MySQL  참가자 검증 + 상대의 hidden 해제      ← 롤백된다
② 파일   FileStorageClient.store (첨부가 있으면) ← 고아 파일은 무해하다
③ Mongo  chat_messages insert                    ← 롤백되지 않는다
④ MySQL  방의 마지막 메시지 요약 갱신           ← 롤백된다
⑤ 커밋 후 STOMP 브로드캐스트
```

> **`storage`·`schedule`의 "되돌릴 수 없는 쪽을 마지막에"와 순서가 다릅니다.**
> 방의 `lastMessageId`는 메시지 id를 참조하는데 그 id는 Mongo가 부여하므로 ④를 ③보다 앞세울 수
> 없습니다. 대신 그 규약이 막으려던 것 — **MySQL이 존재하지 않는 문서를 가리키는 일** — 은 이
> 순서에서도 일어나지 않습니다. ④가 실패하면 트랜잭션이 롤백되어 요약이 갱신되지 않은 채 메시지만
> 남으며, 그 메시지는 방을 열면 정상적으로 보이고 다음 전송이 요약을 바로잡습니다.
>
> ②가 ③보다 앞인 것도 같은 기준입니다 — 고아 파일은 아무에게도 보이지 않지만, 첨부 메타는 있는데
> 실물이 없으면 사용자가 깨진 말풍선을 봅니다.

⑤가 커밋 이후인 이유는 `ChatEventPublisher`에 있습니다. 직접 부르면 뒤이어 롤백될 저장까지
"저장됐다"고 알리게 되는데, **상대 화면에 뜬 말풍선은 되돌릴 방법이 없습니다.**

---

## 실시간 전달

### 목적지는 전부 `/user/queue/...`입니다

| 이벤트 | 클라이언트 구독 | 수신자 |
|---|---|---|
| 새 메시지 | `/user/queue/chat.messages` | 방 참가자 전원(**발신자 포함** — 다른 탭 동기화) |
| 읽음 확인 | `/user/queue/chat.reads` | 상대 1명 |
| 새 방 생성 | `/user/queue/chat.rooms` | 상대 1명(연 사람은 REST 응답으로 이미 받았다) |
| 접속 상태 변경 | `/user/queue/chat.presence` | 같은 테넌트의 **접속 중인** 사람들 |

**`/topic`을 열지 않습니다.** `/topic/chat.room.{roomId}` 같은 공개 목적지가 있으면 아무나 SUBSCRIBE
할 수 있어 roomId를 바꿔가며 남의 대화를 받아볼 수 있고, 그것을 막으려면 구독마다 참가자인지
확인하는 인터셉터가 필요합니다. 사용자별 큐는 Spring이 세션 principal로 목적지를 치환하므로
**남의 것을 구독할 방법이 애초에 없습니다** — 권한 검증이 "발신 시점에 참가자를 읽는 조회 1회"로
끝나고, 그 조회가 `(roomId, tenantId)`라 tenant 격리도 함께 보장됩니다.

수신자 주소는 **username**입니다. Spring의 user destination이 `Principal.getName()`으로 라우팅하고,
이 프로젝트의 principal 이름이 username이기 때문입니다. username은 전 테넌트 전역 유일이라
(`auth`의 `existsByUsername`에 tenant가 없는 이유) 충돌이 없습니다.

### 페이로드에 본문을 싣습니다

SSE의 `SheetsSavedEvent`는 "무엇이 바뀌었는지"만 알리고 본문은 재조회하게 했지만, 채팅은 **본문을
그대로 싣습니다.** 메시지마다 재조회를 유발하면 왕복이 두 배가 되고, 페이로드가 이미 화면에 그릴 것
전부입니다. `tenantId`는 담지 않습니다 — 수신자를 정하는 데만 쓰이는 서버 내부 값입니다.

### 스케일아웃 교체 지점

`ChatEventBroadcaster`(port/out) ← `StompChatEventBroadcaster`(어댑터) 한 겹뿐이고,
**포트 시그니처에 전송 기술이 드러나지 않습니다.** 여러 인스턴스로 늘리면 이 포트를 구현하는
Redis 릴레이를 끼우면 되고 서비스 코드는 바뀌지 않습니다.

지금은 **단일 인스턴스 전제**입니다. 그 한계는 두 곳에 있습니다 —
`StompChatEventBroadcaster`(자기 프로세스의 브로커에만 발행)와
`ChatSessionEventListener`(세션 레지스트리가 힙에만 있음)입니다.
`RedisPresenceStore`의 집합 자체는 인스턴스가 늘어도 Redis에서 합쳐집니다.

---

## 쓰기는 REST, WebSocket은 수신 전용

`ApiResponse` 봉투 · `GlobalExceptionHandler` · `@Valid` · `@AuthenticationPrincipal` · multipart —
**다섯 개가 전부 서블릿 MVC에만** 있습니다. STOMP `SEND`로 받았다면 `@MessageExceptionHandler`,
별도 응답 봉투, principal 언랩, 검증 실패 포맷을 전부 새로 만들어야 했습니다. 첨부가 multipart라
어차피 REST 경로가 필요하니 텍스트도 같은 경로로 통일했습니다.

전송 지연은 `clientMessageId`(클라이언트가 만든 UUID)로 가립니다. 서버가 응답과 브로드캐스트 양쪽에
그대로 돌려주므로 클라이언트가 임시 말풍선을 실제 메시지로 치환합니다. **서버는 해석하지 않습니다.**

그래서 `WebSocketConfig`에 `applicationDestinationPrefixes`가 없습니다 — 클라이언트 SEND를 받지 않는다는
사실이 설정에 드러나 있습니다.

---

## 엔드포인트

### `/api/chat/rooms` — `ChatRoomController`

| 메서드 | 경로 | 비고 |
|---|---|---|
| POST | `/` | **멱등**. 이미 있으면 그 방을 반환 |
| GET | `/` | 최근 대화 순. 감춘 방 제외 |
| GET | `/{roomId}` | 참가자만 |
| POST | `/{roomId}/read` | 커서는 앞으로만 |
| DELETE | `/{roomId}` | **방을 지우지 않고 내 목록에서만 감춥니다** |

### `/api/chat` — `ChatMessageController`

| 메서드 | 경로 | 비고 |
|---|---|---|
| POST | `/rooms/{roomId}/messages` | 텍스트 |
| POST | `/rooms/{roomId}/messages/attachments` | multipart. 10MB 한도 |
| GET | `/rooms/{roomId}/messages?before=&size=` | 커서 페이징. 기본 50, 상한 100 |
| GET | `/rooms/{roomId}/messages/{messageId}/attachment` | **`ResponseEntity<byte[]>`** |
| GET | `/unread-count` | 전역 배지 |

### `/api/chat/contacts` — `ChatContactController`

`/api/admin/members`를 재사용하지 않습니다. 그쪽은 ADMIN 전용이라 일반 사용자가 부를 수 없고
`email`·`tel`까지 내보냅니다.

> **첨부 다운로드는 `ApiResponse` 봉투를 쓰지 않습니다**(루트 규칙 6의 예외, 표의 6번째 행).
> 본문이 JSON이 아니라 파일 바이트이기 때문입니다.

**역할 제한을 두지 않습니다** — 채팅은 테넌트 내부 전원의 기능이고, 격리는 참가자 검증과
tenant 범위 조회가 담당합니다.

---

## 모듈 규칙

### tenant 소유권 격리

루트 `CLAUDE.md` 규칙 13을 따릅니다. 이 모듈에서 주의할 점:

- **격리가 두 겹입니다.** 방 자신의 tenant 범위와, 개설 시 지정하는 **상대가 내 테넌트 소속인지**
  (`ChatRoomValidator.requireSameTenantUser` → `auth`의 `UserQueryUseCase`). 후자를 빠뜨리면 다른
  고객사 사용자와 대화방이 열리고 이름·부서가 그대로 넘어갑니다.
- **참가자가 아닌 접근도 `CHAT_ROOM_NOT_FOUND`입니다.** `CHAT_NOT_PARTICIPANT`를 따로 두지 않은 것은
  의도입니다 — 코드를 나누면 클라이언트가 코드로 방의 존재를 알아낼 수 있어 은닉이 무너집니다.
  서버 로그 구분은 메시지로 합니다.
- **첨부 다운로드는 `roomId`까지 조건에 겁니다.** 참가자 확인만으로는 부족합니다 — 내가 참가한 방의
  id와 남의 방 메시지 id를 조합하면 그 파일을 받아 갈 수 있습니다
  (`ChatMessageRepository.findById(messageId, roomId, tenantId)`).
- **보관소 키(`storageKey`)를 응답에 싣지 않습니다.** 그 자체가 접근 경로가 됩니다.

### 첨부

- 실물 보관은 `global/storage`의 공용 SPI(`FileStorageClient`)입니다. **감싸기만 하는 포트를 이 모듈에
  두지 않습니다**(루트 규칙 3·10의 단순 위임 래퍼 금지). `ChatAttachmentWriter`가 "언제 쓰는가"를
  모아 두는 협력자이지 포트가 아닙니다.
- `storageKey`는 **도메인이 만듭니다**(`ChatAttachment.register`) — `chat/{tenantId}/{roomId}/{yyyyMM}/{UUID}{확장자}`.
  사용자 파일명을 경로에 섞으면 경로 순회와 동명 덮어쓰기가 열립니다.
- **S3에서는 실제 오브젝트 키가 `documents/chat/...`이 됩니다.** `ems.storage.s3.key-prefix` 기본값이
  `documents`이고, **그 값은 바꾸면 기존 파일을 찾지 못하므로 그대로 둡니다.** 버킷 경로일 뿐이라
  기능에는 영향이 없습니다.
- 메시지 종류(`IMAGE`/`FILE`)는 사용자가 고르지 않고 업로드된 `contentType`이 정합니다.
- 본문은 캡션이라 **없어도 됩니다.** 그래서 내용 검사는 텍스트 경로(`ChatMessage.text`)에만 있습니다.

### port/in을 두지 않습니다

타 모듈 소비자가 0입니다. Controller가 `{도메인}Service`를 직접 주입하고 VO는 전부
`application/command/`에 둡니다(`storage`가 `DocumentQueryUseCase`를 두지 않은 것과 같은 판단).
소비자 없는 인터페이스를 `port/in`에 두면 그 반환 VO까지 공개 계약 자리로 끌려 올라갑니다.

### Service 3분할

**애그리거트 축**입니다(CQRS 축이 아닙니다).

| 서비스 | 애그리거트 | 고유 의존 |
|---|---|---|
| `ChatRoomService` | ChatRoom + ChatParticipant | `DirectRoomWriter` |
| `ChatMessageService` | ChatMessage | `ChatAttachmentWriter` |
| `ChatPresenceService` | Presence | `PresenceStore` |

`ChatParticipant`를 따로 가르지 않는 이유는 방의 하위 개념이고 참가자 단독 유스케이스가 없기
때문입니다 — 방을 열면 2건이 함께 생기고, 읽음 커서 갱신은 방 컨텍스트 안입니다.

### 협력자 접미사 `~Publisher` (명시적 예외)

루트 `CLAUDE.md`의 협력자 표에 `~Publisher`는 없습니다. `ChatEventPublisher`가 하는 일은 조립
(`Assembler`)도 탐색(`Finder`)도 동시 쓰기 정책(`Writer`)도 아니라 **트랜잭션 경계 정책**이라
기존 접미사 어디에도 맞지 않아 새로 두었습니다.

두 서비스가 같은 정책을 쓰기 때문에 뽑았습니다. `schedule`은 같은 처리를
`ScheduleSheetService.publishAfterCommit` private 메서드로 두는데, 그쪽은 발행 지점이 하나뿐입니다.

### 세션 리스너가 presentation에 있는 이유

STOMP 세션 이벤트는 **외부에서 들어오는 자극**이라 Controller와 같은 구동(driving) 어댑터입니다.
`infrastructure`에 두면 `infrastructure → application` 역방향이 되고, `application/event/`는 모듈 자기
이벤트의 자리라 스프링 프레임워크 이벤트가 갈 곳이 아닙니다.

세션 레지스트리를 직접 갖는 이유는 스프링의 `SimpUserRegistry`가 username만 주고
`tenantId`·`userId`를 주지 않기 때문입니다.

### 전송 계층은 이 모듈이 아닙니다

`WebSocketConfig`와 `StompAuthChannelInterceptor`는 `global/websocket/`에 있습니다. 판별식은
**"채팅을 삭제하면 이 파일이 남는가"**입니다 — 브로커 설정과 토큰 검증에는 업무 규칙이 한 줄도 없고,
나중에 측정계획 편집 알림이 SSE에서 STOMP로 옮겨오더라도 두 번째 설정이 생겨서는 안 됩니다.

**인증은 CONNECT 프레임에서 한 번만** 합니다. `JwtTokenProvider.getAuthentication`이 users·roles·tenants를
읽어 DB를 3회 타므로(JWT claim에 userId·tenantId가 없습니다) 프레임마다 부르면 메시지 한 건에 그
비용이 붙습니다. 그 대가로 **세션 principal은 CONNECT 시점에 고정**됩니다 — 클라이언트가 액세스
토큰을 갱신할 때마다 재연결하는 것이 계약입니다.

---

## auth 모듈과의 연결

`UserQueryUseCase`(inbound port)로만 접근합니다. 사용자 id는 전부 plain `Long`이며 JPA 연관이 없습니다.

- `getUser(userId, tenantId)` — 상대의 tenant 소속 검증(반환값을 쓰지 않는 호출이 곧 검증입니다)
- `getUserList(tenantId)` — 이름·username을 Map으로 만들어 조회 횟수를 고정
  (`client_management`의 `TeamAssembler`와 같은 방식)

**`UserSummary`를 넓히지 않습니다.** `admin`의 `MemberMapper`가 `unmappedTargetPolicy = ERROR`이며
그것은 의도된 안전장치입니다. 채팅이 필요로 한 세 가지(접속 상태·읽음 위치·아바타)는 전부
`UserSummary` 밖에 두어 이 문제가 발생하지 않습니다.

> **벌크 조회 포트를 추가하지 않은 이유**: `getUserList`가 이미 테넌트 전원을 한 번에 주므로
> `getUsers(List<Long>, tenantId)`를 추가해도 **조회 횟수는 같습니다.** 추가 시점의 기준을 미리
> 정해 둡니다 — 테넌트당 사용자가 1,000명을 넘거나 프로파일링에서 이 조회가 드러나면 그때입니다.

---

## 테스트

`src/test/.../chat/`에 Fake 5개와 서비스 테스트 3개가 있습니다.

| Fake | 재현하는 것 |
|---|---|
| `FakeChatRoomRepository` | `(tenantId, pairKey)` 유니크 → `DataIntegrityViolationException`. `failNextSaveWithConflict()`로 경쟁 재현 |
| `FakeChatParticipantRepository` | 참가자 아님과 방 없음을 구분하지 않는 은닉 |
| `FakeChatMessageRepository` | **id를 0-패딩 단조 증가 문자열로 채번** — 사전순 = 생성순을 재현하지 않으면 커서 테스트가 무의미 |
| `FakePresenceStore` | 세션 집합(다중 탭). TTL은 재현하지 않음 |
| `RecordingChatEventBroadcaster` | 발행된 **(수신자, 페이로드)** 쌍 |
| `RecordingFileStorageClient` | 보관소 호출. 실물 없을 때 `STORAGE_FILE_NOT_FOUND` |

고정한 규칙 중 놓치기 쉬운 것:

- 같은 두 사람으로 두 번 열어도, **인자 순서를 바꿔도** 같은 방
- 유니크 충돌이 나도 상대가 만든 방을 반환
- 미읽음은 **자기가 보낸 메시지를 세지 않는다**
- 커서로 준 메시지는 **다음 페이지에 다시 나오지 않는다**
- 커서는 되돌아가지 않고, **움직였을 때만** 상대에게 알린다
- 탭 두 개 중 하나만 닫으면 여전히 온라인, **마지막 하나가 끊길 때만** 오프라인 알림 1회
- 전송이 거부되면 **알림도 나가지 않는다**
- 다른 방의 `messageId`로는 첨부를 받을 수 없다

---

## 향후 과제

- **멀티 인스턴스**: `StompChatEventBroadcaster`와 `ChatSessionEventListener`의 세션 레지스트리가
  단일 인스턴스 전제입니다. `ChatEventBroadcaster` 뒤에 Redis 릴레이를 끼웁니다
  (`@ConditionalOnProperty`로 고르는 형태는 `storage`의 보관소 선택이 선례입니다).
- **WebSocket 세션 만료 강제**: 지금은 CONNECT 시점 인증이 세션 수명 동안 유지됩니다. 토큰 `exp`를
  세션 속성에 저장해 `@Scheduled`로 끊는 것이 다음 단계입니다(액세스 토큰 유효기간이 1시간이라
  노출 창은 제한적입니다).
- **아바타**: `users` 컬럼이 아니라 별도 `user_profiles`로 갑니다. `UserSummary`는 건드리지 않습니다.
- **첨부 온메모리 `byte[]`**: 10MB 한도에서는 감당되지만, 커지면 스트리밍이나 presigned URL을 검토합니다.
- **썸네일 생성**: 하지 않습니다. 클라이언트가 원본을 받아 축소합니다.
- **메시지 보관 정책**: 무한 축적입니다. TTL 인덱스나 아카이빙이 필요해질 수 있습니다.
- **그룹 채팅**: `chat_rooms`에 `type`(DIRECT/GROUP)과 `name`을 더하면 확장됩니다. `pair_key`는
  DIRECT에만 채우고 UNIQUE는 그대로 둡니다.
- **읽음 확인의 정밀도**: 지금은 "여기까지 읽었다" 커서 하나입니다. 메시지별 읽음 표시가 필요해지면
  참가자 커서를 비교해 계산할 수 있습니다.
