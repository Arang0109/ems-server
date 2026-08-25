# EMS 데이터베이스 스키마

멀티테넌시(Multi-tenancy) 전환 이후의 전체 테이블 정의입니다. JPA 엔티티(`*/infrastructure/entity/`) 기준으로 작성되었으며, DDL은 Hibernate가 생성합니다.

## 테넌시 모델 개요

최상위에 **`tenants`(테넌트/고객사)** 가 있고, 거의 모든 업무 테이블이 `tenant_id`로 테넌트에 소속됩니다.

```
tenants (테넌트/고객사)
├── users              (사용자)            tenant_id, role_id
│   └── roles ──< role_privileges >── privileges   (역할·권한)
├── clients            (의뢰기관)          tenant_id
│   └── workplaces     (사업장)            tenant_id, client_id
│       └── stacks     (측정시설/굴뚝)     tenant_id, workplace_id
│           ├── facilities        (배출시설)      tenant_id, stack_id
│           └── preventions       (방지시설)      tenant_id, stack_id
├── pollutants         (고객사 채택 물질)  tenant_id, catalog_id
├── stack_pollutant    (시설별 측정물질)   tenant_id, stack_id, pollutant_id
├── contract           (계약)              tenant_id, workplace_id
└── schedules          (측정계획 메타)     tenant_id, stack_id, team_id
    ├── schedule_documents (MongoDB 세부 스냅샷)  scheduleId 로 연결
    └── analysis_records   (MongoDB 실험분석정보) scheduleId 로 연결
```

**전역(테넌트 비종속) 테이블**
- `roles`, `privileges`, `role_privileges` — 권한 체계
- **`pollutant_catalog`** — 고객사에게 지원하는 측정물질 가이드. 모든 테넌트가 같은 `code`로 물질을 식별할 수 있게 합니다. 표기명·시험장비·시험방법은 가이드가 아니라 `pollutants`(고객사)가 소유합니다.

**`tenant_id` 연관 방식 두 가지**
- **JPA 연관(@ManyToOne → TenantEntity)**: `clients`, `workplaces`, `stacks`, `facilities`, `preventions`, `pollutants`, `stack_pollutant` — 실제 FK(`fk_*_tenants`) + `ON DELETE CASCADE`.
- **plain 컬럼(Long, FK 제약 없음)**: `users`, `contract` — `tenant_id`를 값으로만 보유(모듈 경계상 TenantEntity에 의존하지 않음). 애플리케이션이 정합성 보장.

---

## tenants — 테넌트(고객사)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| tenant_id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR | NOT NULL | 테넌트명 |
| biz_number | VARCHAR(10) | UNIQUE | 사업자번호 |
| status | VARCHAR | ENUM(String) | `TenantStatus` |
| subscription_plan | VARCHAR | ENUM(String) | `SubscriptionPlan` |
| created_at | DATETIME | NOT UPDATABLE | 생성 시각 |
| modified_at | DATETIME | | 수정 시각 |

---

## users — 사용자

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| user_id | BIGINT | PK, AUTO_INCREMENT | |
| tenant_id | BIGINT | NOT NULL | 소속 테넌트(plain 컬럼, FK 제약 없음) |
| role_id | BIGINT | NOT NULL, FK→roles | `fk_users_roles`, ON DELETE CASCADE |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 로그인 아이디 |
| password | VARCHAR | NOT NULL | 암호화 비밀번호 |
| name | VARCHAR | NOT NULL | 이름 |
| department | VARCHAR(50) | | 부서 |
| email | VARCHAR(100) | | |
| tel | VARCHAR(20) | | |
| created_at | DATETIME | NOT UPDATABLE | Auditing |
| modified_at | DATETIME | | Auditing |

---

## roles — 역할

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| role_id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(50) | NOT NULL, UNIQUE | 역할명 |
| description | VARCHAR | NOT NULL | 설명 |

## privileges — 권한

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| privilege_id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(50) | NOT NULL, UNIQUE | 권한명 |
| description | VARCHAR | NOT NULL | 설명 |

## role_privileges — 역할·권한 매핑(N:M 조인)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| role_privilege_id | BIGINT | PK, AUTO_INCREMENT | |
| role_id | BIGINT | NOT NULL, FK→roles | `fk_role_privileges_roles`, ON DELETE CASCADE |
| privilege_id | BIGINT | NOT NULL, FK→privileges | `fk_role_privileges_privileges`, ON DELETE CASCADE |

- **UNIQUE** `uk_role_privileges` (role_id, privilege_id)
- **INDEX** `idx_role_privileges_role_id` (role_id)

---

## clients — 의뢰기관

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| client_id | BIGINT | PK, AUTO_INCREMENT | |
| tenant_id | BIGINT | NOT NULL, FK→tenants | `fk_clients_tenants`, ON DELETE CASCADE |
| name | VARCHAR | NOT NULL | 의뢰기관명 |
| biz_number | VARCHAR(10) | | 사업자번호 |
| representative | VARCHAR | | 대표자 |
| road_address | VARCHAR | | 도로명주소 |
| detail_address | VARCHAR | | 상세주소 |
| zipcode | VARCHAR | | 우편번호 |
| manager | VARCHAR | | 담당자 |
| email | VARCHAR | | |
| tel | VARCHAR | | |
| remark | LONGTEXT | | 비고 |
| created_at / modified_at | DATETIME | | |

- **UNIQUE** `uk_clients_tenants` (tenant_id, name)
- **INDEX** `idx_clients_tenant_id` (tenant_id)

---

## workplaces — 사업장

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| workplace_id | BIGINT | PK, AUTO_INCREMENT | |
| tenant_id | BIGINT | NOT NULL, FK→tenants | `fk_workplaces_tenants`, ON DELETE CASCADE |
| client_id | BIGINT | NOT NULL, FK→clients | `fk_workplaces_clients`, ON DELETE CASCADE |
| name | VARCHAR | NOT NULL | 사업장명 |
| biz_number | VARCHAR(10) | | 사업자번호 |
| road_address | VARCHAR | | 도로명주소 |
| detail_address | VARCHAR | | 상세주소 |
| zipcode | VARCHAR | | 우편번호 |
| grade | VARCHAR | ENUM(String) | `Grade` |
| created_at / modified_at | DATETIME | | |

- **UNIQUE** `uk_workplaces_client_name` (client_id, name)
- **INDEX** `idx_workplaces_tenant_id` (tenant_id)

---

## stacks — 측정시설(굴뚝)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| stack_id | BIGINT | PK, AUTO_INCREMENT | |
| tenant_id | BIGINT | NOT NULL, FK→tenants | `fk_stacks_tenants`, ON DELETE CASCADE |
| workplace_id | BIGINT | NOT NULL, FK→workplaces | `fk_stacks_workplaces`, ON DELETE CASCADE |
| field | VARCHAR | NOT NULL, ENUM(String) | `MeasurementField` (측정분야) |
| name | VARCHAR | NOT NULL | 시설명 |
| sems_number | VARCHAR | | SEMS 번호 |
| grade | VARCHAR | ENUM(String) | `Grade` |
| business_category | VARCHAR | | 업종 |
| main_product | VARCHAR | | 주생산품 |
| height | VARCHAR | | 높이 |
| horizontal_length | VARCHAR | | 가로 |
| vertical_length | VARCHAR | | 세로 |
| shape | VARCHAR | ENUM(String) | `Shape` |
| orientation | VARCHAR | ENUM(String) | `Orientation` |
| created_at / modified_at | DATETIME | | Auditing(@EntityListeners) |

- **UNIQUE** `uk_stacks_workplace_name_field` (workplace_id, name, field)
- **INDEX** `idx_stacks_tenant_id` (tenant_id)

---

## facilities — 배출시설

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| facility_id | BIGINT | PK, AUTO_INCREMENT | |
| tenant_id | BIGINT | NOT NULL, FK→tenants | `fk_facilities_tenants`, ON DELETE CASCADE |
| stack_id | BIGINT | NOT NULL, FK→stacks | `fk_facilities_stacks`, ON DELETE CASCADE |
| name | VARCHAR | NOT NULL | 시설명 |
| fuel_usage | VARCHAR | | 연료 사용량 |
| product_output | VARCHAR | | 제품 생산량 |
| incineration_amount | VARCHAR | | 소각량 |
| fuel_input | VARCHAR | | 원료 투입량 |
| fuel_type | VARCHAR | | 연료·원료 종류 |
| unit | VARCHAR | | 사용량·생산량에 적용되는 단위 |
| sort_order | INT | | 측정지점 안에서의 표시 순서. 10 단위 |
| created_at / modified_at | DATETIME | | |

- **UNIQUE** `uk_facilities_stack_name` (stack_id, name)
- **INDEX** `idx_facilities_tenant_id` (tenant_id)
- **INDEX** `idx_facilities_stack_sort` (stack_id, sort_order)
- `sort_order`는 nullable이라 조회 시 항상 tie-breaker를 동반합니다 — `ORDER BY sort_order ASC, facility_id ASC`. 신규 등록은 서버가 `max(sort_order) + 10`을 부여해 목록 맨 뒤에 붙입니다. 순서 변경은 `PUT /api/facilities/order`가 목록 전체를 10 단위로 재부여합니다.

---

## preventions — 방지시설

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| prevention_id | BIGINT | PK, AUTO_INCREMENT | |
| tenant_id | BIGINT | NOT NULL, FK→tenants | `fk_preventions_tenants`, ON DELETE CASCADE |
| stack_id | BIGINT | NOT NULL, FK→stacks | `fk_preventions_stacks`, ON DELETE CASCADE |
| name | VARCHAR | NOT NULL | 시설명 |
| capacity | DOUBLE | | 용량 |
| unit | VARCHAR | | 용량 단위 |
| target_name | VARCHAR | | 대상물질명 |
| removal_efficiency | VARCHAR | | 제거효율 |
| sort_order | INT | | 측정지점 안에서의 표시 순서. 10 단위 |
| created_at / modified_at | DATETIME | | |

- **UNIQUE** `uk_preventions_stack_name` (stack_id, name)
- **INDEX** `idx_preventions_tenant_id` (tenant_id)
- **INDEX** `idx_preventions_stack_sort` (stack_id, sort_order)
- `sort_order`는 nullable이라 조회 시 항상 tie-breaker를 동반합니다 — `ORDER BY sort_order ASC, prevention_id ASC`. 신규 등록은 서버가 `max(sort_order) + 10`을 부여해 목록 맨 뒤에 붙입니다. 순서 변경은 `PUT /api/preventions/order`가 목록 전체를 10 단위로 재부여합니다.
- 대상물질은 별도 테이블이 아니라 `target_name`·`removal_efficiency` 두 컬럼으로 방지시설에 직접 보유합니다(방지시설당 1건).

---

## pollutant_catalog — 측정물질 가이드 (전역 마스터)

고객사에게 **지원하는 측정물질 가이드**입니다. **`tenant_id`가 없는 전 테넌트 공유 테이블**로, `roles`/`privileges`와 같은 범주입니다.

가이드는 "무엇을 쓸 수 있는가"만 정의합니다. 영문명·시험장비·시험방법 같은 **고객사 표기값은 보유하지 않습니다** — 그 값들은 `pollutants`가 직접 관리합니다.

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| catalog_id | BIGINT | PK, AUTO_INCREMENT | |
| code | VARCHAR(30) | NOT NULL | 물질 판별 키(`NOX`, `SOX`, `PB` …). 클라이언트 분기의 기준. **측정분야 안에서만 유일** |
| field | VARCHAR | NOT NULL, ENUM(String) | `MeasurementField` |
| name_kr | VARCHAR | NOT NULL | 가이드 표준 한글명. 고객사가 채택할 때 **복사해 가는 초기값** |
| method | VARCHAR | ENUM(String) | `MeasurementMethod` |
| phase | VARCHAR | ENUM(String) | `PollutantPhase` |
| sort_order | INT | | 목록 표시 순서(법령 고시 순서) |
| active | BOOLEAN | NOT NULL | 폐지 항목은 false. **하드 삭제 금지** — 채택 행·스냅샷이 참조 |
| created_at / modified_at | DATETIME | | |

- **UNIQUE** `uk_pollutant_catalog_field_code` (field, code)
- **INDEX** `idx_pollutant_catalog_field` (field, sort_order)
- **수정의 파급 범위가 컬럼마다 다릅니다.**
  - `field`·`method`·`phase`: 가이드가 단일 진실 소스이며 `pollutants`에는 컬럼이 없습니다. 조회 시 조인으로 전파되므로 **이미 채택한 고객사에도 즉시 반영**됩니다.
  - `name_kr`: 채택 시점에 복사되는 초기값이므로 **앞으로 채택할 고객사에만** 반영됩니다. 이미 채택한 고객사는 자신의 표기명을 보유하므로 바뀌지 않습니다.
- **code의 유일 범위가 측정분야인 이유**: 같은 물질이라도 대기와 수질은 배출허용기준·공정시험법이 다른 별개 항목입니다. 납·카드뮴·수은 등 중금속은 양쪽 법령에 모두 있으므로, code를 전역 유일로 두면 한쪽만 등록할 수 있습니다. 그래서 `(AIR, PB)`와 `(WATER, PB)`가 공존합니다.
  - 따라서 **code만으로는 물질을 특정할 수 없습니다.** 다른 계층(측정물질 생성, 시설별 측정물질 등록)이 카탈로그를 지목할 때는 `catalog_id`를 씁니다. code는 클라이언트가 화면(측정분야) 안에서 물질을 분기하는 용도입니다.
- 시드는 `src/main/resources/catalog/pollutant-catalog.json` + `PollutantCatalogInitializer`(멱등, `pollutant.catalog.seed-enabled`)로 주입합니다. 운영 중 추가·수정은 `PLATFORM_ADMIN` 전용 `/api/platform/pollutant-catalog`에서 합니다.
- `code`는 부여 후 변경하지 않습니다. 클라이언트 분기와 측정계획 스냅샷이 이 값에 의존합니다.
- ⚠️ **현재 시드는 `AIR` 48건뿐입니다.** `pollutants.catalog_id`가 NOT NULL이므로 가이드에 항목이 없는 측정분야는
  물질을 하나도 등록할 수 없습니다. `WATER`·`NOISE_VIBRATION`·`ODOR` 분야를 열려면 **카탈로그 확충이 선행되어야 합니다.**

---

## pollutants — 측정물질 (고객사 채택 물질)

고객사가 가이드에서 **채택한** 측정물질입니다. 가이드에 없는 물질은 만들 수 없으므로 `catalog_id`는 **NOT NULL**입니다.

여기 있는 컬럼은 전부 **고객사 소유값**이며, 고객사가 직접 입력·관리합니다.
`field`·`method`·`phase`는 가이드가 단일 진실 소스이므로 컬럼으로 두지 않고 조회 시 조인으로 채웁니다
(`PollutantEntityMapper.toDomain`이 `code`와 함께 투영).

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| pollutant_id | BIGINT | PK, AUTO_INCREMENT | |
| tenant_id | BIGINT | NOT NULL, FK→tenants | `fk_pollutants_tenants`, ON DELETE CASCADE |
| catalog_id | BIGINT | **NOT NULL**, FK→pollutant_catalog | `fk_pollutants_catalog`. **ON DELETE 없음**(카탈로그는 폐지만 함) |
| name_kr | VARCHAR | NOT NULL | 한글명. 채택 시 가이드 값을 복사하며 이후 고객사가 관리 |
| name_en | VARCHAR | | 영문명. 고객사 입력값(초기 공백) |
| equipment | VARCHAR | | 시험장비. 고객사 입력값(초기 공백) |
| test_method | VARCHAR | | 시험방법. 고객사 입력값(초기 공백) |
| created_at / modified_at | DATETIME | | |

- **UNIQUE** `uk_pollutants_tenant_catalog` (tenant_id, catalog_id) — 한 테넌트가 같은 가이드 항목을 두 번 채택할 수 없습니다. 중복 채택은 이 제약 하나로 막습니다.
- **INDEX** `idx_pollutants_tenant_id` (tenant_id)
- ⚠️ **`(tenant_id, name_kr)` UNIQUE는 두지 않습니다.** 채택 시 가이드 국문명을 복사하므로, 한 고객사가 분야가 다른
  동명 물질(대기 납·수질 납)을 함께 쓰면 정상 요청이 거부됩니다. `field` 컬럼이 없어 `(tenant, field, name_kr)`로
  좁힐 수도 없습니다.
- 이 행은 고객사가 **명시적으로 채택**할 때만 만들어집니다(`POST /api/pollutants`). 시설별 측정물질 등록이 행을 대신 만들어 주지 않습니다 —
  고객사가 등록하지 않은 물질이 관리 목록에 나타나지 않게 하기 위해서입니다.
  `name_kr`을 비워 채택하면 가이드 국문명이 복사되고, 나머지 입력값은 이후 고객사가 채웁니다.
- 폐지된(`active=false`) 가이드 항목은 **새로 채택할 수 없습니다**(`POLLUTANT_CATALOG_INACTIVE`). 이미 채택한 물질은 계속 씁니다.

---

## stack_pollutant — 시설별 측정물질

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| stack_pollutant_id | BIGINT | PK, AUTO_INCREMENT | |
| tenant_id | BIGINT | NOT NULL, FK→tenants | `fk_stackPollutant_tenants`, ON DELETE CASCADE |
| stack_id | BIGINT | NOT NULL, FK→stacks | `fk_stackPollutant_stacks`, ON DELETE CASCADE |
| pollutant_id | BIGINT | NOT NULL, FK→pollutants | `fk_stackPollutant_pollutants` |
| cycle | VARCHAR | ENUM(String) | `MeasurementCycle` (측정주기) |
| allowance | DECIMAL | | 허용치 |
| oxygen_applicable | BOOLEAN | | 기준산소농도 적용 여부 |
| created_at / modified_at | DATETIME | | |

- **UNIQUE** `uk_tenantId_stackId_pollutantId` (tenant_id, stack_id, pollutant_id)
- **INDEX** `idx_stackPollutant_tenantId` (tenant_id)
- 참조 대상은 카탈로그가 아니라 `pollutants`입니다. 등록 요청은 **이미 채택한 `pollutant_id`만** 받습니다. 아직 채택하지 않은 가이드 항목은 `POST /api/pollutants`로 먼저 채택해야 합니다.

---

## contract — 계약

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| tenant_id | BIGINT | NOT NULL | 소속 테넌트(plain 컬럼, FK 제약 없음) |
| workplace_id | BIGINT | NOT NULL | 사업장(plain 컬럼, FK 제약 없음) |
| contract_name | VARCHAR | | 계약명 |
| contract_date | DATE | | 계약일 |
| start_date | DATE | | 착수일 |
| completion_date | DATE | | 완료일 |
| contract_amount | DECIMAL | | 계약금액 |
| contract_amount_unit | INT | ENUM(**Ordinal**) | `ContractAmountUnit` — ⚠️ `@Enumerated` 미지정으로 ORDINAL 저장됨 |
| vat_included | BIT/BOOLEAN | | 부가세 포함 여부 |
| contract_guarantee_amount | DECIMAL | | 계약보증금 |
| advance_payment_amount | DECIMAL | | 선급금 |
| advance_payment_due_date | INT | | 선급금 지급기한(일) |
| delay_penalty_rate | INT | | 지연배상률 |
| remark | VARCHAR | | 비고 |

- audit 컬럼 없음. 인덱스/유니크 제약 미정의.

---

## schedules — 측정계획(메타)

측정계획은 **메타(MySQL) + 세부 스냅샷(MongoDB)** 하이브리드입니다. MySQL이 진실의 원천이고,
MongoDB `schedule_documents`는 측정 시점의 대상·팀·장비·측정항목 스냅샷과 측정 시트(실측값·계산결과)를 담습니다.

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| schedule_id | BIGINT | PK, AUTO_INCREMENT | |
| tenant_id | BIGINT | NOT NULL | 소속 테넌트(plain 컬럼, FK 제약 없음) |
| stack_id | BIGINT | NOT NULL | 측정 대상 시설(plain 컬럼, FK 제약 없음) |
| team_id | BIGINT | NOT NULL | 측정 팀(plain 컬럼, FK 제약 없음) |
| measurement_field | VARCHAR | NOT NULL, ENUM(String) | `MeasurementField` |
| sampled_at | DATE | NOT NULL | 측정(채취) 일자. 측정 건수 집계 기준일 |
| schedule_purpose | VARCHAR | | 측정 용도(자가측정용 등) |
| status | VARCHAR | NOT NULL, ENUM(String) | `ScheduleStatus` |
| reference_number | VARCHAR | | 내부 식별 코드 |
| created_at / modified_at | DATETIME | | `@EntityListeners(AuditingEntityListener)` 부착 |

- **UNIQUE** `uk_schedules_stack_team_date` (tenant_id, stack_id, team_id, sampled_at)
- **INDEX** `idx_schedules_tenant_id` (tenant_id)

> 생애주기는 `status` 하나로 관리합니다. 업무가 실재했으나 무산된 건은 **취소**(`CANCELED`)로 목록에
> 남기고, 애초에 잘못 등록된 건은 **삭제**로 지웁니다 — 삭제는 행을 지우는 물리 삭제이며
> 세부 문서(`schedule_documents`)와 실험분석정보(`analysis_records`)도 함께 지워 되돌릴 수 없습니다.

> 시료접수·분석완료·성적서발행 일자와 채취 시작/종료 시각은 MySQL이 아니라
> MongoDB `schedule_documents.basicInfo`에 있습니다. MySQL의 날짜 컬럼은 `sampled_at` 하나뿐입니다.

### 측정 시트 동시 편집 (낙관적 락)

한 측정계획을 두 명 이상이 동시에 입력할 수 있으므로, `PUT /api/schedules/{id}/sheets`는
요청 시트를 보관본에 **병합**합니다(`SheetMerge`). 전체 배열을 통째로 덮어쓰지 않습니다.

| 단위 | 토큰 | 위치 |
|------|------|------|
| 시트(`MeasurementSheet`) | `version` (Long, 신규는 null → 0) | 문서 안 `sheets[].version` |
| 문서(`ScheduleDocument`) | `@Version version` (Spring Data MongoDB) | `schedule_documents.version` |

- **충돌 판정 단위는 시트**입니다. 시트에는 식별자가 없고 `category`(GAS/HEAVY_METAL/DUST/MERCURY)가
  자연키이므로 이를 키로 삼습니다. 두 사람이 서로 다른 시트를 나눠 입력하면 충돌이 나지 않고
  양쪽 입력이 모두 남습니다. 같은 시트를 건드렸을 때만 409(`SCHEDULE_SHEET_VERSION_CONFLICT`)로 거부합니다.
- **요청에 없는 카테고리의 시트는 그대로 둡니다.** 그래서 시트 삭제는 요청의 `deletedSheets`
  (`SheetRef{category, version}`)로 명시해야 합니다 — 요청에서 빠졌다는 것만으로는 "내가 지웠다"와
  "다른 사용자가 방금 추가했다"를 구분할 수 없기 때문입니다. 삭제도 편집이므로 version을 함께 판정합니다.
- **문서 `@Version`은 물리적 동시 저장을 막는 안전망**입니다. 여기 걸리면 논리적 충돌이 아니라 저장이
  겹친 것이므로, 서버가 문서를 다시 읽어 병합을 재시도합니다(최대 3회, `ScheduleService.mergeAndSaveSheets`).
- 버전 도입 전에 저장된 **시트**(`sheets[].version == null`)는 판정 대상이 아니며, 첫 저장에서 0을 받습니다.
- ⚠️ **문서 `version` 은 배포 전 백필이 필수입니다.** Spring Data 는 `@Version` 이 null 인 문서를
  *신규*로 판정해 `insert` 를 시도하므로, 필드가 없는 기존 문서는 같은 `_id` 로 insert 가 나가
  `E11000 duplicate key` 로 **모든 저장이 실패**합니다. 필드 타입이 래퍼(`Long`)라 0 은 신규로
  보지 않으므로 0 으로 채우면 됩니다(primitive `long` 이었다면 이 방법이 통하지 않습니다).

  ```bash
  mongosh "mongodb://<host>:27017/ems" --file docs/migration/2026-08-18-schedule-document-version.js
  ```
- 클라이언트는 응답의 `snapshot.sheets[].version`을 그대로 되돌려 보내야 합니다.
  `version`·`createdAt`은 도메인 스냅샷(`ScheduleSnapshot`)이 왕복시키며, 저장할 때마다
  `@LastModifiedDate`가 `modifiedAt`만 새로 채웁니다.
- **문서 단위 `version`은 응답에 나가지 않습니다.** 물리 충돌은 서버가 다시 읽어 재시도로 흡수하므로
  클라이언트가 왕복시킬 필요가 없습니다. 응답이 왕복시키는 낙관적 락 토큰은 `sheets[].version` 하나뿐입니다.

### 상태(`ScheduleStatus`) 전이

전이 규칙은 `ScheduleStatus.canTransitionTo()`가 강제합니다. 단계를 건너뛸 수 없고 되돌릴 수 없으며,
유일한 예외가 재개방(`reopen`)입니다.

```
SCHEDULED ──> MEASURING ──> ANALYZING ──> REPORT_COMPLETED  (종단)
     └─────────────┴─────────────┴───────> CANCELED         (종단)

재개방:  REPORT_COMPLETED | CANCELED ──> SCHEDULED ──(스냅샷에서 단계 재도출)──> 원래 단계
```

업무 단계는 측정예정 → 측정중 → 인계완료 → 분석값입력중 → 분석완료 → 성적서작성완료 6단계지만,
**인계완료와 분석값입력중이 같은 시점**이고 **분석완료와 성적서작성완료도 같은 시점**이라
각각 하나로 합쳐 `ANALYZING`·`REPORT_COMPLETED`로 표현합니다.

전이 트리거는 **"전진은 자동, 종료는 수동"** 원칙을 따릅니다. 상태는 사용자의 의도가 아니라
업무의 사실을 기록해야 하므로, 사실 신호가 있는 전진은 서버가 자동으로 처리합니다.

| 전이 | 트리거 | 처리 위치 |
|------|--------|-----------|
| `SCHEDULED → MEASURING` | 채취 시작시각(`basicInfo.samplingStartedAt`) **또는** 측정점 실측값(`ts`·`pv`·`ps` 중 하나) | `ScheduleProgress.advance` 자동 |
| `MEASURING → ANALYZING` | 시료접수일자(`basicInfo.receivedAt`) | `ScheduleProgress.advance` 자동 |
| `ANALYZING → REPORT_COMPLETED` | 사용자 확정 | `POST /api/schedules/{id}/completion` |
| `* → CANCELED` | 사용자 확정 | `POST /api/schedules/{id}/cancellation` |
| 종단 → `SCHEDULED` → 재도출 | 사용자 확정 | `POST /api/schedules/{id}/reopen` |

- **시트를 저장한 것만으로는 전진하지 않습니다.** 틀만 있고 측정점 값이 비어 있으면 `SCHEDULED`에 머뭅니다.
- 자동 전진은 `updateBasicInfo`·`saveSheets`·`changeEquipments`·`changeClient`·`changeItems`·`updateItem`
  **6개 경로 모두**에서 판정되며, 멱등하고 역행하지 않습니다. 한 번의 판정에서 2단계를 건너뛰지 않고
  순차 적용하므로 두 신호가 동시에 충족되면 `SCHEDULED → ANALYZING`까지 연쇄 전진합니다.
- `REPORT_COMPLETED`·`CANCELED`는 종단 상태이며 `canEdit() == false`입니다. 모든 수정 경로가
  `Schedule.requireEditable()`에서 409(`SCHEDULE_NOT_EDITABLE`)로 차단됩니다.
  **삭제는 별개 규칙(`canDelete()`)** 이라 `SCHEDULED`·`CANCELED`에서만 허용됩니다.
- 측정 건수 통계(`countCompleted` 등)는 `REPORT_COMPLETED`이면서 `sampled_at`이 있는 건만 집계합니다.
  재개방하면 상태가 종단을 벗어나므로 통계에서도 자동으로 빠집니다.
- `status`는 MongoDB 문서에도 사본으로 보관되며(`syncStatus`), 응답·통계는 항상 MySQL 값을 씁니다.
  그래서 **문서 쪽 사본은 응답에 실리지 않습니다** — `ScheduleResponse` 최상위의 `status` 하나만 내려갑니다.
- ⚠️ **`COMPLETED` → `REPORT_COMPLETED` 개편(2026-08-22)은 배포 전 치환이 필수입니다.** 상태는 MySQL·이력·Mongo
  모두 문자열로 저장되므로 옛 값이 남아 있으면 읽는 순간 enum 파싱에서 예외가 납니다. 구버전이 새 값을
  읽어도, 신버전이 옛 값을 읽어도 터지므로 **무중단 배포가 불가능**합니다 —
  구버전 중지 → 아래 두 스크립트 실행 → 신버전 배포 순서로 진행합니다.

  ```bash
  mysql -u <user> -p ems < docs/migration/2026-08-22-schedule-status-report-completed.sql
  mongosh "mongodb://<host>:27017/ems" --file docs/migration/2026-08-22-schedule-status-report-completed.js
  ```

  SQL 스크립트 첫머리의 CHECK 제약 조회를 먼저 확인하세요. `ddl-auto=update` 는 기존 CHECK 제약을
  갱신하지 않으므로, 제약이 남아 있으면 `UPDATE` 자체가 거부됩니다.

### 상태 변경 이력 (제거됨, 2026-08-25)

`schedule_status_logs` 테이블과 `schedules.deleted_at`·`deleted_by`는 제거했습니다. 상태 변경 이력을
쌓지 않으므로 취소·재개방 사유(`reason`)도 받지 않으며, 두 엔드포인트의 요청 본문이 사라졌습니다.
soft delete가 없어지면서 삭제는 물리 삭제로 바뀌었고, 복구 경로
(`GET /api/schedules/deleted`, `POST /api/schedules/{id}/restore`)도 함께 없앴습니다.

⚠️ **배포 전 DDL이 필요합니다.** `ddl-auto=update`는 컬럼·제약·테이블 삭제를 반영하지 않습니다.

```bash
mysql -u <user> -p ems < docs/migration/2026-08-25-schedule-drop-status-log.sql
```

---

## analysis_records — 실험분석정보 (MongoDB)

한 측정계획의 측정항목 하나에 대한 **성적서용 기록**입니다. **측정항목 하나당 문서 하나**로 저장하며,
현장 측정값(측정 시트)이 아니라 측정이 끝난 뒤 별도로 입력됩니다.

**한 문서를 두 화면이 필드를 나눠 소유합니다.** 실험·분석 탭은 실험실 입력값
(`analysisValue`·`unit`·`analysisMethod`·`analysisEquipment`)만, 성적서 탭은 채취시간
(`samplingStartedAt`·`samplingEndedAt`)만 씁니다. 저장 경로도 갈라져 있어
(`PUT /analyses/results` vs `PUT /analyses/sampling-times`) 두 탭을 동시에 열어도
서로의 입력을 덮어쓰지 않습니다.

**두 일괄 저장 모두 `pollutantId`를 키로 upsert 합니다.** 문서 id로 신규·기존을 판별하게 두면
한 탭이 문서를 만든 사실을 다른 탭이 모른 채 등록을 시도해 409로 막힙니다(화면이 탭을 언마운트하지
않아 목록을 다시 읽지 않습니다). 실제 불변식이 "한 계획의 한 측정항목 = 문서 하나"이므로
대리키가 아니라 자연키로 쓰는 편이 맞습니다.

| 필드 | 타입 | 설명 |
|------|------|------|
| _id | ObjectId | 문서 id. API 경로의 `analysisId` |
| tenantId | Long | 소속 테넌트 |
| scheduleId | Long | 소속 측정계획. **조회의 축** |
| stackPollutantId | Long | 원장(`stack_pollutant`) 연결키. 스냅샷에서 복사 |
| pollutantId | Long | 측정물질. **계획 안에서 분석 기록의 유일성 축** |
| pollutantName | String | 측정물질명. 스냅샷에서 복사 |
| allowance | Decimal | **허용기준치**. 측정 시점 원장 사본(스냅샷) |
| oxygenApplicable | Boolean | **기준산소농도 보정 적용 여부**. 측정 시점 원장 사본(스냅샷) |
| analysisValue | Decimal | **측정분석값** |
| unit | String | **측정단위** |
| analysisMethod | String | **측정분석방법** |
| analysisEquipment | String | **분석장비** |
| samplingStartedAt | Time | **채취 시작시각**. 성적서 탭 작성분 |
| samplingEndedAt | Time | **채취 종료시각**. 성적서 탭 작성분 |
| createdAt / modifiedAt | DateTime | `@CreatedDate` / `@LastModifiedDate` |

- **INDEX** `idx_analysis_tenant_schedule` (tenantId, scheduleId)
- `allowance`·`oxygenApplicable`은 등록 시 `schedule_documents.items[]`(측정 시점 stack_pollutant 사본)에서
  복사하며 **수정 대상이 아닙니다.** 원장의 허용기준이 개정되어도 과거 회차의 초과 판정이 뒤집히면 안 되기 때문이며,
  `measurement_records.result`가 같은 이유로 판정 근거를 행에 고정하는 것과 같은 방침입니다.
- 계획의 측정항목이 아닌 물질은 400(`SCHEDULE_ITEM_NOT_IN_SCHEDULE`), 같은 항목 중복 등록은
  409(`SCHEDULE_ANALYSIS_ALREADY_EXISTS`)로 거부합니다. 재분석은 등록이 아니라 수정(PUT)으로 처리합니다.
- **`schedule_documents`와 컬렉션을 분리한 이유**: 실험실 입력이 측정 시트 저장의 문서 단위 낙관적 락
  (`schedule_documents.version`)과 부딪히지 않게 하기 위해서입니다. 현장 측정과 실험실 분석은
  서로 다른 시점·다른 담당자가 입력합니다.
- 완료·취소된 계획에는 등록·수정·삭제가 모두 막힙니다(`Schedule.requireEditable()`).
- `analysisValue`는 **필수가 아닙니다.** 성적서 탭이 채취시간만 먼저 저장해 둔 문서가 정상 상태이기 때문입니다.
- **채취시간은 현장 채취 기록지(`schedule_documents.sheets[]`)에서 자동으로 옮겨오지 않습니다.**
  기록지는 알데히드류를 `VOCs`로 통칭해 시료 한 건으로 적지만 성적서는 포름알데히드·아세트알데히드를
  각각 씁니다(시료 1건 ↔ 항목 N건). 그 통칭 규칙이 업체마다 달라 서버가 고정할 수 없으므로,
  자동 복사는 조용히 틀린 시각을 성적서에 남깁니다. 그래서 성적서 탭에서 항목별로 직접 작성합니다.
- 채취시간 일괄 저장은 **전달한 항목만** 갱신합니다. 전달한 항목의 빈 시각은 기존 값을 지우며
  (표의 빈 칸 = "지웠다"), 요청에 없는 항목은 그대로 둡니다. 시각이 둘 다 비어 있고 기존 문서도 없으면
  문서를 만들지 않습니다. 한 요청에 같은 항목이 두 번 담기면 400(`SCHEDULE_ANALYSIS_DUPLICATE_ITEM`)입니다.
- 시작·종료 시각의 순서는 검증하지 않습니다 — 자정을 넘겨 채취하는 회차(23:00→01:00)가 정상적으로 있습니다.

| 메서드 | 경로 |
|--------|------|
| POST | `/api/schedules/{scheduleId}/analyses` |
| GET | `/api/schedules/{scheduleId}/analyses` |
| GET | `/api/schedules/{scheduleId}/analyses/{analysisId}` |
| PUT | `/api/schedules/{scheduleId}/analyses/{analysisId}` |
| PUT | `/api/schedules/{scheduleId}/analyses/results` — 항목별 실험분석 결과 일괄 저장 |
| PUT | `/api/schedules/{scheduleId}/analyses/sampling-times` — 성적서 항목별 채취시간 일괄 저장 |
| DELETE | `/api/schedules/{scheduleId}/analyses/{analysisId}` |

> `results`·`sampling-times`는 리터럴 경로라 같은 `PUT`의 `/{analysisId}`보다 먼저 매칭됩니다
> (Spring `PathPattern` 특이성 우선). 컨트롤러에서도 위에 선언해 두었으니 순서를 바꾸지 마세요.
> 회귀는 `AnalysisRecordControllerRoutingTest`가 잡습니다.

- 단건 `POST`·`PUT /{analysisId}`도 남아 있지만 화면은 쓰지 않습니다. 두 일괄 저장과 달리 `POST`는
  같은 항목이 이미 있으면 409(`SCHEDULE_ANALYSIS_ALREADY_EXISTS`)로 거부하고, `PUT /{analysisId}`의
  null은 "지움"이 아니라 "기존 값 유지"입니다 — 부분 수정용이라 규칙이 다릅니다.

---

## measurement_records — 측정항목별 회차 이력

"어느 회차에 어느 측정항목을 측정했고 값이 얼마였는가"를 한 행으로 남깁니다.
측정계획이 **성적서 작성 완료(`REPORT_COMPLETED`)될 때** 측정항목 수만큼 추가되고, **재개방되면 그 계획의 행이 통째로 삭제**됩니다.

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| record_id | BIGINT | PK, AUTO_INCREMENT | |
| tenant_id | BIGINT | NOT NULL | plain 컬럼, FK 제약 없음 |
| stack_id | BIGINT | NOT NULL | 측정시설. plain 컬럼 |
| schedule_id | BIGINT | NOT NULL | 이 이행을 만든 측정계획. 재개방 시 이 값으로 되돌립니다 |
| stack_pollutant_id | BIGINT | | 원장 연결키. 측정항목 식별자 도입 이전 스냅샷은 비어 있습니다 |
| pollutant_id | BIGINT | NOT NULL | 측정물질. **조회·유일성의 축** |
| pollutant_code | VARCHAR(30) | | 측정 시점 가이드 키 스냅샷 |
| pollutant_name_kr | VARCHAR | | 측정 시점 물질명 스냅샷 |
| cycle | VARCHAR | ENUM(String) | 측정 시점 주기. 원장에서 주기 미지정이면 NULL |
| period_year | INT | NOT NULL | 구간 연도. 주기가 없어도 측정일의 연도가 들어갑니다 |
| period_index | INT | | 1부터 시작하는 구간 번호. 주기가 없으면 NULL |
| sampled_at | DATE | NOT NULL | 채취일자. **구간 판정의 기준일** |
| completed_at | DATETIME | NOT NULL | 완료 확정 시각. 도메인이 값을 정하므로 auditing 리스너를 쓰지 않습니다 |
| concentration / unit | DECIMAL(19,6) / VARCHAR(30) | | 실측 농도와 단위 |
| corrected_concentration | DECIMAL(19,6) | | 산소보정 후 농도. 보정 미적용 항목은 NULL |
| emission | DECIMAL(19,6) | | 배출량 |
| allowance | DECIMAL(19,6) | | 측정 시점 허용기준 스냅샷 |
| exceeded | BOOLEAN | | 허용기준 초과 여부. 기준이나 비교값이 없어 판정 불가면 NULL |

- **UNIQUE** `uk_measurement_records_schedule_pollutant` (schedule_id, pollutant_id) — 회차당 물질 1행. 재완료를 멱등하게 만듭니다
- **INDEX** `idx_measurement_records_stack_period` (tenant_id, stack_id, period_year, period_index)
- **INDEX** `idx_measurement_records_stack_pollutant` (tenant_id, stack_id, pollutant_id, sampled_at) — 항목별 추이 조회
- **INDEX** `idx_measurement_records_tenant_year` (tenant_id, period_year) — 미이행 집계

> **결과값 컬럼은 아직 채워지지 않습니다.** 항목별 분석 결과 입력 경로가 도입되기 전까지 이행 사실만 기록되며,
> 결과 컬럼은 전부 NULL입니다. 컬럼을 미리 둔 이유는 이행과 결과가 "그 회차에 이 항목을 측정했다"는 하나의
> 사실이어서 나중에 테이블을 쪼갤 이유가 없기 때문입니다.

### 측정 시점 값을 복사해 두는 이유

`cycle`·`allowance`·`pollutant_code`·`pollutant_name_kr`은 원장(`stack_pollutant`·`pollutants`)에서 조인해 올 수 있는데도
행에 복사합니다. 원장의 주기나 허용기준이 개정되면 **과거의 이행 판정과 초과 판정이 소급해서 뒤집히기** 때문입니다.
측정계획 스냅샷(`SamplingItemSnapshot`)이 이미 같은 이유로 측정 시점 값을 들고 있으며, 이 이력은 원장이 아니라
그 스냅샷에서 값을 가져옵니다.

### 조회 축이 `pollutant_id`인 이유

`stack_pollutant`에는 수정 API가 없어 **주기 변경이 곧 삭제 후 재등록**이고, 그때 `stack_pollutant_id`가 새로 발급되어
이력이 끊깁니다. `pollutant_id`는 그 조작에도 그대로이므로 추이 조회의 안정적인 키입니다.

또한 FK를 걸지 않으므로 측정시설·측정항목이 삭제돼도 이력은 남습니다. 이력은 "그때 그런 측정이 있었다"는 사실이며,
이행 현황판은 *현재 등록된* 항목만 행 축으로 삼으므로 삭제된 항목이 현황판에 유령으로 나타나지 않습니다.

### 주기 이행 판정 규칙

역년(1월 시작) 고정으로 구간을 나눕니다. 시설·항목별 기준월은 두지 않습니다 — 법정 자가측정이 역년 기준으로 부과되므로
구간을 시설마다 굴리면 오히려 규정과 어긋납니다.

| cycle | 연간 구간 수 | 구간당 필요 횟수 | 구간 키 예 |
|-------|-------------|-----------------|-----------|
| MONTHLY | 12 | 1 | `2026-M03` |
| TWICE_MONTHLY | 12 | **2** | `2026-T03` |
| BIMONTHLY | 6 | 1 | `2026-B2` |
| QUARTERLY | 4 | 1 | `2026-Q1` |
| SEMI_ANNUAL | 2 | 1 | `2026-H1` |
| ANNUAL | 1 | 1 | `2026-Y` |

- "월 2회"는 구간이 아니라 **한 구간에 필요한 횟수**입니다. 모든 주기를 `(구간 수, 필요 횟수)` 쌍으로 환원했기 때문에
  판정이 `기록 수 >= 필요 횟수` 한 식으로 끝납니다.
- 구간 키에 주기 표식(`M`/`T`/`B`/`Q`/`H`/`Y`)을 넣는 이유는 원장의 주기가 도중에 바뀔 수 있어서입니다.
  접두가 없으면 같은 `(연도, 번호)`가 월 주기와 격월 주기에서 다른 뜻인데 같은 키가 되어, 과거 기록이 새 주기의
  엉뚱한 칸을 채웁니다.
- 판정 결과는 `FULFILLED` / `PARTIAL` / `PENDING`(기한 전) / `OVERDUE`(기한 경과)입니다. 아직 오지 않은 구간까지
  미이행 경고로 칠하면 연초에 현황판이 온통 붉어져 정작 놓친 구간이 묻히므로 기한 전후를 구분합니다.
- 구간 판정 기준일은 **`sampled_at`(채취일)** 이지 완료일이 아닙니다. 12월 측정을 1월에 완료해도 12월 구간 이행입니다.
- 계산 로직은 `schedule/domain/history/MeasurementPeriod`가 소유합니다. `MeasurementCycle`(global enum)에는 두지 않습니다 —
  global에는 기능성 코드를 두지 않으며, 무엇이 한 구간인지는 이력을 소유한 모듈의 업무 규칙입니다.

---

## Enum 값 참조

`@Enumerated(EnumType.STRING)`로 저장(계약의 `ContractAmountUnit` 제외).

| Enum | 값 |
|------|-----|
| `TenantStatus` | ACTIVE, SUSPENDED, INACTIVE, PENDING |
| `SubscriptionPlan` | BASIC, PRO, ENTERPRISE, INTERNAL |
| `Grade` | TYPE_1 ~ TYPE_5 |
| `MeasurementField` | AIR(대기), WATER(수질), NOISE_VIBRATION(소음진동), ODOR(악취) |
| `MeasurementMethod` | DUST, HEAVY_METAL, MERCURY, FIELD_MEASUREMENT, ABSORPTION_SOLUTION, ADSORPTION_TUBE, TEDLAR_BAG, CARTRIDGE |
| `PollutantPhase` | PARTICLE(입자상), GAS(가스상) |
| `MeasurementCycle` | MONTHLY, TWICE_MONTHLY, BIMONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUAL |
| `Shape` | CIRCULAR, RECTANGULAR |
| `Orientation` | VERTICAL, HORIZONTAL |
| `ContractAmountUnit` | MONTH, QUARTER, SEMI_ANNUAL, ANNUAL, TOTAL |
| `ScheduleStatus` | SCHEDULED(측정예정), MEASURING(측정중), ANALYZING(분석값입력중), REPORT_COMPLETED(성적서작성완료), CANCELED(취소) |

---

## 참고 / 후속 과제

1. **측정물질 스키마는 초기화 기준입니다**: `pollutants`가 카탈로그 오버라이드에서 **고객사 채택 물질**로 바뀌면서
   `catalog_id` NOT NULL 추가, `field`/`method`/`phase` 컬럼 제거, 레거시 유니크(`name_kr`/`name_en` 전역 UNIQUE) 제거가 함께 이뤄졌습니다.
   `ddl-auto: update`는 **컬럼 삭제·nullability 변경·제약 삭제를 수행하지 않으므로**, 이전 스키마가 반영된 DB는
   `pollutants`·`pollutant_catalog`를 **drop 후 재생성**하는 편이 확실합니다(개발 단계 기준. 운영 DB는 2026-08-17 초기화 완료).
   재기동하면 Hibernate가 두 테이블을 새로 만들고 `PollutantCatalogInitializer`가 AIR 48건을 시드합니다.
   ```sql
   -- 구 스키마가 남아 있는 개발 DB에서만
   SET FOREIGN_KEY_CHECKS = 0;
   DROP TABLE IF EXISTS stack_pollutant, pollutants, pollutant_catalog;
   SET FOREIGN_KEY_CHECKS = 1;
   -- 재기동 후 확인
   -- SELECT COUNT(*) FROM pollutant_catalog;   -- 48
   ```
2. **`contract.contract_amount_unit` ORDINAL 저장**: `@Enumerated(EnumType.STRING)` 부재. enum 순서 변경 시 데이터 깨짐 위험 → STRING 저장 권장.
3. **Auditing 리스너 범위**: `@EntityListeners(AuditingEntityListener.class)`가 없으면 `created_at`/`modified_at`이 항상 null로 저장됩니다. `stacks`, `users`, `pollutant_catalog`, `pollutants`, `stack_pollutant`에는 부착돼 있으나, `clients`·`workplaces`·`facilities`·`preventions`·`teams` 등 나머지 테넌트 테이블은 아직 누락 상태입니다.
4. **tenant_id 주입 경로**: 인증된 사용자의 `tenant_id`(`users.tenant_id`)를 `CustomUserDetails.tenantId`로 로드하여, 컨트롤러가 `@AuthenticationPrincipal`로 읽어 생성 커맨드에 주입합니다.
5. **`target_substances` 테이블 수동 삭제 필요**: 측정대상물질은 `preventions.target_name`/`removal_efficiency`로 통합되어 엔티티가 제거됐지만, `ddl-auto: update`는 테이블/컬럼 삭제를 반영하지 않습니다. 기존 DB에 남아 있는 `target_substances` 테이블은 `DROP TABLE target_substances;`로 직접 정리해야 합니다.
