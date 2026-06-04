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
| `CompanyService` | `getCompanyList` | 전체 의뢰기관 목록 조회 |

---

## 향후 구현 예정

### Workplace
- `WorkplaceService`, `WorkplaceController`
- 생성 / 조회 / 수정 / 삭제
- Company ID 기준 사업장 목록 조회

### Stack
- `StackService`, `StackController`
- 생성 / 조회 / 수정 / 삭제
- Workplace ID 기준 굴뚝 목록 조회

### 공통
- Company / Workplace 수정 및 삭제
- 사업자번호 중복 검증 (`CompanyRepository.existsByBizNumber`)
- 페이징 (`Pageable`) 적용
- 입력값 검증 (`@Valid`)

---

## 알려진 아키텍처 이슈


---

## 모듈 규칙

### Repository Port
- `domain/port/CompanyRepository` — 현재 `save()`, `findAll()` 구현
- Workplace, Stack 추가 시 각각 `domain/port/WorkplaceRepository`, `domain/port/StackRepository`를 추가합니다.
- 부모-자식 조회는 반드시 Port 메서드로 추상화합니다. (예: `findByCompanyId(Long companyId)`)

### 연관 관계 조회
- Company 조회 시 Workplace, Stack을 즉시 로드하지 않습니다.
- 필요한 경우 별도 서비스 메서드로 Workplace, Stack을 조회합니다.
