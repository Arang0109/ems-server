# client_management 모듈 가이드라인

측정대행 의뢰 기관(업체), 사업장, 굴뚝(Stack)을 관리하는 모듈입니다.

## 도메인 모델 구조

```
Company (의뢰기관)
  └── Workplace (사업장)  1:N
        └── Stack (굴뚝/측정기기)  1:N
```

### 도메인 모델 책임
| 모델 | 책임 |
|------|------|
| `Company` | 측정대행을 의뢰하는 기관. 사업자번호 기준 식별 |
| `Workplace` | Company 산하의 개별 사업장. 자체 사업자번호 보유 |
| `Stack` | Workplace 산하의 개별 굴뚝/측정기기. 측정 분야·형태·등급 등 물리적 속성 포함 |

### Enum 위치
Stack의 속성에서 사용하는 Enum(`Grade`, `MeasurementField`, `Shape`, `Orientation`)은 `global/common/enums/`에서 관리합니다.

---

## 현재 구현된 유스케이스

| 서비스 | 메서드 | 설명 |
|--------|--------|------|
| `CompanyService` | `createCompany` | 의뢰기관 신규 등록 |
| `CompanyService` | `getCompany(companyId)` | 의뢰기관 단건 조회 |
| `CompanyService` | `getCompanyList` | 전체 의뢰기관 목록 조회 |
| `CompanyService` | `updateCompany(companyId, command)` | 의뢰기관 수정 |
| `CompanyService` | `deleteCompany(companyId)` | 의뢰기관 삭제 |
| `WorkplaceService` | `createWorkplace` | 사업장 신규 등록 |
| `WorkplaceService` | `getWorkplace(workplaceId)` | 사업장 단건 조회 |
| `WorkplaceService` | `getWorkplaceList(companyId)` | Company ID 기준 사업장 목록 조회 |
| `WorkplaceService` | `updateWorkplace(workplaceId, command)` | 사업장 수정 |
| `WorkplaceService` | `deleteWorkplace(workplaceId)` | 사업장 삭제 |
| `StackService` | `createStack` | 측정시설(굴뚝) 신규 등록 |
| `StackService` | `getStack(stackId)` | 측정시설 단건 조회 |
| `StackService` | `getStackList(workplaceId)` | Workplace ID 기준 측정시설 목록 조회 |
| `StackService` | `updateStack(stackId, command)` | 측정시설 수정 |
| `StackService` | `deleteStack(stackId)` | 측정시설 삭제 |

---

## 향후 구현 예정

### 공통
- 페이징 (`Pageable`) 적용

---

## 알려진 아키텍처 이슈


---

## 모듈 규칙

### Repository Port
- `domain/port/CompanyRepository` — `save()`, `findById()`, `findAll()`, `deleteById()`
- `domain/port/WorkplaceRepository` — `save()`, `findById()`, `findByCompanyId()`, `deleteById()`
- `domain/port/StackRepository` — `save()`, `findById()`, `findByWorkplaceId()`, `deleteById()`
- 부모-자식 조회는 반드시 Port 메서드로 추상화합니다. (예: `findByCompanyId(Long companyId)`)
- `findById()` 는 `Optional` 을 반환하지 않고 `T` 를 직접 반환합니다. 없으면 Adapter에서 `NOT_FOUND` 예외를 던집니다.

### 목록 조회 VO 패턴
- 목록 조회 시 연관 도메인 정보(부모명 등)를 포함하는 VO는 `application/command/`에 Record로 정의합니다.
  - `WorkplaceListItem` — `companyName` 포함 사업장 목록 아이템
  - `StackListItem` — `companyName`, `workplaceName` 포함 측정시설 목록 아이템
- Infrastructure 매퍼(`{도메인}EntityMapper`)가 JPA 엔티티 → VO 변환을 담당합니다.
- Presentation 매퍼(`{도메인}Mapper`)가 VO → Response DTO 변환을 담당합니다.
  - 메서드명: `toListResponse()` / `toListResponses()`

### 연관 관계 조회
- Company 조회 시 Workplace, Stack을 즉시 로드하지 않습니다.
- 필요한 경우 별도 서비스 메서드로 Workplace, Stack을 조회합니다.
