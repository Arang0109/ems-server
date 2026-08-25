# contract 모듈 가이드라인

측정대행 계약을 관리하는 모듈입니다. 단일 애그리거트(`Contract`)로 구성된 작은 모듈이지만,
사업장(`client_management`)에 종속되고 통계를 `dashboard`에 공급하는 두 방향의 연결을 갖습니다.

---

## 애그리거트

| 애그리거트 | 저장소 | 도메인 루트 | 비고 |
|---|---|---|---|
| **Contract** | MySQL `contracts` | `domain/Contract` | tenant 종속. `tenant_id`·`workplace_id` 모두 plain `Long` 컬럼 |

계약은 **사업장에 매달립니다.** 사업장이 사라지면 그 계약도 남을 이유가 없습니다(아래 이벤트 절 참고).

### 읽기 모델 — `ContractTableViewEntity`

목록 조회는 `contracts` 테이블이 아니라 **`@Subselect` DB 뷰**에서 읽습니다.
계약 목록 화면이 사업장명·의뢰기관명·측정분야를 함께 보여 줘야 하는데, 이들은 `client_management`
소유라 JPA 조인을 걸 수 없기 때문입니다. 모듈 경계를 지키면서 N+1을 피하는 방법으로 뷰를 씁니다.

- `@Immutable` + `@Synchronize({"contracts","workplaces","clients","stacks"})` — 쓰기는 하지 않고,
  Hibernate가 관련 테이블의 pending 변경을 먼저 flush하도록 강제합니다.
- 매핑은 `infrastructure/mapper/ContractListItemMapper`가 담당하며 `ContractListItem`(VO)을 만듭니다.

> **`TableView`는 UI 용어가 아닙니다.** 루트 규칙 7이 금지하는 것은 응답 DTO의 `Table`·`Grid` 같은
> 렌더링 방식 명명이며, 여기서는 **DB 뷰(view)**를 가리키는 read-model 엔티티명입니다.
> 다만 오해 소지가 있으므로 새 read-model에는 `~ViewEntity`보다 명확한 이름을 고려하세요.

---

## 유스케이스

`ContractService` 하나가 전부이며 두 개의 inbound port를 구현합니다.

| 메서드 | 비고 |
|---|---|
| `createContract` | **사업장 존재 확인을 `WorkplaceQueryUseCase.existsById(workplaceId, tenantId)`로** 합니다. tenant까지 좁히지 않으면 타 고객사 사업장에 계약을 붙일 수 있습니다 |
| `getContract` / `updateContract` / `deleteContract` | 전부 `(contractId, tenantId)` |
| `getContractList(workplaceId, tenantId)` | `workplaceId`가 null이면 tenant 전체, 아니면 그 사업장 것만 |
| `deleteContracts(workplaceId)` | 사업장 삭제 이벤트의 캐스케이드 (아래 참고) |
| `countContracts` / `countContractsInMonth` / `findExpiringBetween` | `dashboard`에 공급하는 통계 |

`ContractDetailAssembler`가 `Contract` 도메인에 사업장 요약을 결합해 `ContractDetail`을 만듭니다.
사업장 정보는 `WorkplaceQueryUseCase`로 가져옵니다 — JPA 조인이 아니라 포트 호출입니다.

---

## 모듈 간 연결

### 나가는 방향 — `client_management`

`WorkplaceQueryUseCase`(inbound port)로만 접근합니다. `WorkplaceRepository`(outbound port)나
엔티티를 직접 참조하지 않습니다.

### 들어오는 방향 — `dashboard`

`application/port/in/ContractStatisticsUseCase`와 `ExpiringContractSummary`를 공개합니다.
`ContractListItem`(내부 VO) → `ExpiringContractSummary`(공개 계약) 변환은
`application/mapper/ContractSummaryMapper`가 담당합니다 — 루트 규칙 3의 **포트 계약 생산 매퍼**입니다.

### 사업장 삭제 캐스케이드

`application/event/WorkplaceDeletedEventListener`가 `client_management`의 `WorkplaceDeletedEvent`를 받아
그 사업장의 계약을 지웁니다. **프로젝트에서 유일한 크로스모듈 이벤트 소비처입니다.**

- 이벤트는 발행 모듈이 소유하는 **공개 계약**입니다. `client_management`가 이 이벤트의 형태를 바꾸면
  여기가 깨집니다.
- `deleteByWorkplaceId(workplaceId)`에 tenantId가 없는 이유: 사업장 삭제 자체가 이미 tenant 범위에서
  일어난 뒤의 내부 캐스케이드이고, `workplaceId`가 그 tenant에 종속된 값이기 때문입니다.
  **이 메서드를 외부 API에서 직접 호출하지 마세요.**

---

## 모듈 규칙

### tenant 소유권 격리

루트 `CLAUDE.md` 규칙 13을 따릅니다. 이 모듈은 2026-08-25에 격리를 **전 경로에 적용**했습니다.

| 경로 | 적용 |
|---|---|
| 단건 조회·수정·삭제 | `findById(id, tenantId)` / `deleteById(id, tenantId)` → `ContractJpaRepository.findByContractIdAndTenantId` 등 |
| 목록 | `findByWorkplaceId(workplaceId, tenantId)` — **부모 id만 받고 tenant를 안 걸면 타 고객사 계약 목록이 노출됩니다** |
| 생성 | `WorkplaceQueryUseCase.existsById(workplaceId, tenantId)` |

- 컨트롤러는 `principal.getTenantId()`를 **실제로 서비스에 전달**해야 합니다.
  파라미터만 선언하고 쓰지 않으면 시그니처만 안전해 보입니다 — 실제로 그런 상태였던 적이 있습니다.
- 소유권 불일치는 404 `NOT_FOUND`입니다.

### 포트 위치 — `domain/port/` 잔존

`ContractRepository`가 아직 `domain/port/`에 있습니다. 표준 위치는 `application/port/out/`입니다(루트 규칙 4).

**이 위치 때문에 규칙 1 위반이 하나 딸려 있습니다** — `ContractRepository`가 목록 반환 타입으로
`application/command/ContractListItem`을 참조해 `domain → application` 역방향 의존이 됩니다.
포트를 `application/port/out/`으로 옮기면 이 문제도 함께 사라집니다.

### presentation 구조

`ContractController`가 **`presentation/` 직하**에 있습니다. 다른 모든 모듈은 `presentation/controller/`를 씁니다.
단일 애그리거트 모듈이라 허용 범위이긴 하나 프로젝트에서 유일한 형태이므로 `controller/` 아래로 옮길 예정입니다.

---

## 향후 과제

- `ContractRepository`를 `application/port/out/`으로 이관 (역방향 의존 동시 해소)
- `ContractController`를 `presentation/controller/`로 이동
- `ContractQueryUseCase.deleteContracts`는 이름이 `Query`인데 삭제를 수행합니다.
  이벤트 캐스케이드 전용 계약이므로 `ContractLifecycleUseCase` 같은 이름이 정확합니다
- `createContract`의 사업장 미존재가 범용 `NOT_FOUND`입니다. `CONTRACT_WORKPLACE_NOT_FOUND`처럼
  도메인 코드를 두면 클라이언트가 무엇이 없는지 구분할 수 있습니다(루트 규칙 12)
- `countContracts`·`countContractsInMonth`가 **전체 목록을 읽어 메모리에서 셉니다.**
  계약이 늘면 COUNT 쿼리로 바꿔야 합니다
- 이 모듈에는 테스트가 없습니다. tenant 격리 회귀 방지가 우선입니다
