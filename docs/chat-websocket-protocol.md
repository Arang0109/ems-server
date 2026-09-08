# 채팅 프로토콜 계약 (ems-web ↔ ems-server)

프론트엔드가 채팅을 붙일 때 필요한 규격입니다. 서버 쪽 설계 근거는
`src/main/java/com/ensolution/ems/chat/.claude/CLAUDE.md`에 있습니다.

**한 줄 요약: 쓰기는 REST, WebSocket은 수신 전용입니다.**

---

## 1. 연결

```ts
import { Client } from "@stomp/stompjs";

const client = new Client({
  brokerURL: `${WS_BASE}/ws`,                                      // wss://{API_HOST}/ws
  connectHeaders: { Authorization: `Bearer ${getAccessToken()}` },
  reconnectDelay: 1000,
  heartbeatIncoming: 25000,
  heartbeatOutgoing: 25000,
});
client.activate();
```

- **라이브러리는 `@stomp/stompjs` 하나입니다.** `sockjs-client`는 넣지 않습니다 — 서버가 SockJS 폴백을
  켜지 않습니다.
- **토큰은 URL에 넣지 않습니다.** CONNECT 프레임 헤더는 WebSocket 페이로드 본문이라 액세스 로그·
  Referer·프록시 로그 어디에도 남지 않습니다. `entities/schedule/api/stream.ts`가 SSE에서
  `EventSource`를 버린 것과 같은 근거를 만족합니다.
- **하트비트 25초는 서버와 맞춘 값입니다.** nginx `proxy_read_timeout` 기본 60초 안에 프레임이 오가야
  유휴 소켓이 끊기지 않습니다.

인증에 실패하면 서버가 STOMP `ERROR` 프레임을 보내고 소켓을 닫습니다. 사유는 구분해 알려 주지 않습니다.

---

## 2. 구독 (연결당 1회)

**방을 열 때마다 구독하지 않습니다.** 방별 목적지가 없고, 목적지는 사용자별 큐 4개뿐입니다.

| 목적지 | 언제 오는가 |
|---|---|
| `/user/queue/chat.messages` | 새 메시지. **내가 보낸 것도 온다**(다른 탭·기기 동기화) |
| `/user/queue/chat.reads` | 상대가 내 메시지를 읽음 |
| `/user/queue/chat.rooms` | 상대가 나에게 대화방을 열었음 |
| `/user/queue/chat.presence` | 누군가의 접속 상태가 바뀜 |

```ts
client.onConnect = () => {
  client.subscribe("/user/queue/chat.messages", (frame) => onMessage(JSON.parse(frame.body)));
  client.subscribe("/user/queue/chat.reads",    (frame) => onRead(JSON.parse(frame.body)));
  client.subscribe("/user/queue/chat.rooms",    (frame) => onRoomOpened(JSON.parse(frame.body)));
  client.subscribe("/user/queue/chat.presence", (frame) => onPresence(JSON.parse(frame.body)));
};
```

### 페이로드

**STOMP 프레임에는 `ApiResponse` 봉투가 없습니다.** 본문이 곧 이벤트 객체입니다.

```jsonc
// chat.messages
{ "roomId": 12, "messageId": "665f...c2", "senderId": 37, "senderName": "김철수",
  "type": "TEXT" | "IMAGE" | "FILE",
  "content": "3번 굴뚝 채취 끝났습니다" | null,
  "attachment": { "filename": "보고서.pdf", "contentType": "application/pdf", "size": 20481 } | null,
  "clientMessageId": "a3f1-..." | null,
  "sentAt": "2026-09-08T14:03:11" }

// chat.reads
{ "roomId": 12, "readerId": 37, "lastReadMessageId": "665f...c2", "readAt": "2026-09-08T14:03:20" }

// chat.rooms
{ "roomId": 12, "peerUserId": 37, "peerName": "김철수", "peerDepartment": "측정1팀" }

// chat.presence
{ "userId": 37, "status": "ONLINE" | "OFFLINE", "changedAt": "2026-09-08T14:00:00" }
```

`chat.messages`는 **본문을 그대로 싣습니다** — 받고 나서 다시 조회할 필요가 없습니다.
`senderId`가 내 `userId`와 같으면 내가 보낸 메아리입니다.

---

## 3. 전송 (REST)

STOMP `SEND`를 쓰지 않습니다. 서버에 `applicationDestinationPrefixes`가 아예 없습니다.

| 메서드 | 경로 | 본문 |
|---|---|---|
| POST | `/api/chat/rooms` | `{ counterpartId }` — **멱등**. 이미 있으면 그 방을 반환 |
| GET | `/api/chat/rooms` | — 최근 대화 순. 감춘 방 제외 |
| GET | `/api/chat/rooms/{roomId}` | — |
| GET | `/api/chat/rooms/{roomId}/messages?before=&size=` | — 최신순. 기본 50, 상한 100 |
| POST | `/api/chat/rooms/{roomId}/messages` | `{ content, clientMessageId }` |
| POST | `/api/chat/rooms/{roomId}/messages/attachments` | `multipart`: `request`(JSON) + `file` |
| GET | `/api/chat/rooms/{roomId}/messages/{messageId}/attachment` | — **바이너리**(봉투 없음) |
| POST | `/api/chat/rooms/{roomId}/read` | `{ lastReadMessageId }` |
| DELETE | `/api/chat/rooms/{roomId}` | — 내 목록에서만 감춤 |
| GET | `/api/chat/unread-count` | — 전역 배지 |
| GET | `/api/chat/contacts` | — 대화 상대 목록(`online` 포함) |

다운로드를 뺀 전부 `ApiResponse` 봉투입니다.

### 낙관적 UI — `clientMessageId`

전송 전에 UUID를 만들어 보내면 서버가 **응답과 브로드캐스트 양쪽에 그대로 돌려줍니다.**
응답을 기다리는 동안 띄워 둔 임시 말풍선을 이 키로 실제 메시지에 치환하세요. 서버는 해석하지 않습니다.

### 첨부

```ts
const form = new FormData();
form.append("request", new Blob([JSON.stringify({ content, clientMessageId })],
  { type: "application/json" }));
form.append("file", file);
```

- **10MB 한도**입니다. 넘으면 `413 CHAT_ATTACHMENT_TOO_LARGE`.
- `content`는 캡션이라 **없어도 됩니다.**
- `type`(IMAGE/FILE)은 서버가 `contentType`으로 정합니다. 클라이언트가 지정하지 않습니다.
- 다운로드 응답에는 `Content-Disposition`이 붙습니다(파일명 UTF-8 인코딩).

---

## 4. 커서 페이징

```
GET /api/chat/rooms/12/messages?size=50            → 최신 50건
GET /api/chat/rooms/12/messages?before={nextCursor}&size=50   → 그 위 50건
```

응답: `{ messages: [...최신순...], nextCursor: string | null, hasMore: boolean }`

**offset을 쓰지 않는 이유**: 읽는 동안 앞에 새 메시지가 붙어 offset이 밀립니다 — 위로 스크롤할수록
이미 본 메시지가 다시 나오거나 건너뜁니다. `nextCursor`가 `null`이면 끝입니다.

---

## 5. 재연결

1. `reconnectDelay`로 자동 재연결(지수 백오프 상한 30초 권장). SSE 클라이언트의 백오프 정책과 맞춥니다.
2. **재연결할 때마다 `connectHeaders`의 accessToken을 새로 읽습니다.** 만료된 토큰을 계속 재시도하면
   영영 실패합니다.
3. CONNECT가 `ERROR`로 끊기면 → `POST /api/auth/refresh` **1회** → 재연결. 그래도 실패하면 로그인 화면
   (`stream.ts`의 `MAX_AUTH_RETRIES` 규약과 동일).
4. **accessToken을 갱신할 때마다 STOMP를 재연결**합니다(`deactivate()` → `activate()`).
   서버 세션의 principal은 CONNECT 시점에 고정되므로, 갱신 없이 두면 만료된 신원으로 계속 붙어 있게 됩니다.
5. **로그아웃 시 반드시 `deactivate()`.** 서버가 Redis의 Refresh Token을 지워도 이미 열린 소켓은
   끊기지 않습니다.
6. **재연결 후 놓친 메시지는 REST로 메꿉니다** — 열려 있는 방의 마지막 `messageId` 이후를
   `GET /messages`로 조회합니다. 알림은 유실될 수 있고 **조회 결과가 늘 진실의 원천**입니다.

---

## 6. 화면별 데이터 흐름

| 화면 | 초기 로드 | 실시간 갱신 |
|---|---|---|
| 연락처 목록 | `GET /api/chat/contacts` (`online` 포함) | `chat.presence`. 화면 재진입 시 재조회(이벤트 유실 대비) |
| 대화방 목록 | `GET /api/chat/rooms` (`unreadCount`·`lastMessage` 포함) | `chat.messages` 수신 시 해당 방 항목 갱신 |
| 대화방 | `GET /rooms/{id}/messages?size=50` → 위로 스크롤 시 `?before=` | `chat.messages`(해당 `roomId`만), `chat.reads` |
| 전역 배지 | `GET /api/chat/unread-count` | `chat.messages` 수신 시 +1, read 전송 후 재조회 |

### 읽음 표시

읽음 커서는 **둘 다** 응답에 실립니다. 쓰임이 다릅니다.

| 필드 | 쓰임 | 어디에 |
|---|---|---|
| `myLastReadMessageId` | **어디부터 보여 줄까** — 방에 들어갈 때 안 읽은 첫 메시지로 점프 | `GET /rooms`, `GET /rooms/{id}`, `POST /rooms` |
| `peerLastReadMessageId` | **내 말풍선에 "읽음"을 붙일까** — 이 id 이하인 내 메시지에 표시 | 같음 |

**둘을 바꿔 쓰지 마세요.** 뒤바뀌면 상대가 읽지 않은 메시지에 "읽음"이 붙습니다.
같은 방이라도 누가 조회하느냐에 따라 두 값이 서로 뒤바뀌어 옵니다 — 언제나 `my`는 요청자 것입니다.

보고: 방을 열고 스크롤이 바닥이면 `POST /rooms/{id}/read { lastReadMessageId }`.
서버가 상대에게 `chat.reads`를 보냅니다.

- **커서는 앞으로만 갑니다** — 위로 스크롤해 옛 메시지를 봐도 이전 위치를 보내지 마세요(서버가 무시합니다).
- **`lastReadMessageId`는 서버가 준 24자 16진 메시지 id입니다.** `clientMessageId`(임시 말풍선 키)를
  보내면 **400 `CHAT_INVALID_MESSAGE_ID`** 입니다. 가장 흔한 실수라 서버가 형식을 검사합니다.

---

## 7. 오류 코드

| 코드 | 상태 | 언제 |
|---|---|---|
| `CHAT_ROOM_NOT_FOUND` | 404 | 방이 없거나, **내가 참가자가 아니거나**, 다른 테넌트의 방 |
| `CHAT_MESSAGE_NOT_FOUND` | 404 | 메시지가 없거나 그 방의 메시지가 아님 |
| `CHAT_INVALID_MESSAGE_ID` | 400 | 메시지 id 형식이 아님(24자 16진). `clientMessageId` 를 보낸 경우가 대부분 |
| `CHAT_SELF_ROOM_NOT_ALLOWED` | 400 | 자기 자신과 대화방을 열려 함 |
| `CHAT_MESSAGE_EMPTY` | 400 | 내용도 첨부도 없음 |
| `CHAT_ATTACHMENT_TOO_LARGE` | 413 | 첨부 10MB 초과 |
| `CHAT_ATTACHMENT_NOT_FOUND` | 404 | 첨부가 없는 메시지의 다운로드 |
| `USER_NOT_FOUND` | 404 | 상대가 없거나 다른 테넌트 소속 |

> **403은 나오지 않습니다.** 참가자가 아닌 접근도 404입니다 — 코드를 나누면 클라이언트가 방의
> 존재를 알아낼 수 있기 때문입니다. "권한 없음"과 "없음"을 화면에서 구분하려 하지 마세요.

---

## 8. 배포 환경 (nginx)

```nginx
location /ws {
    proxy_pass http://backend;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_read_timeout 3600s;
}
```

`Upgrade`/`Connection` 헤더가 없으면 핸드셰이크가 **조용히** 실패합니다 — API 호출에는 아무 흔적이
남지 않아 원인을 찾기 어렵습니다.

허용 오리진은 서버의 `global/security/config/AllowedOrigins`가 REST(CORS)와 핸드셰이크 양쪽에
같은 값을 공급합니다. 새 오리진을 추가할 때는 그 한 곳만 고치면 됩니다.

---

## 9. 지금은 단일 인스턴스 전제입니다

서버를 여러 대로 늘리면 다른 인스턴스에 붙은 사용자에게 알림이 가지 않습니다. 프론트가 대비할 것은
없지만, **알림 유실을 전제로 만든 5번의 재연결 규칙(특히 6번 항목)이 그때도 안전망이 됩니다.**
