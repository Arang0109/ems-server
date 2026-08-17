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
    └── schedule_documents (MongoDB 세부 스냅샷)  scheduleId 로 연결
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
| fuel_input | VARCHAR | | 연료 투입량 |
| fuel_type | VARCHAR | | 연료 종류 |
| created_at / modified_at | DATETIME | | |

- **UNIQUE** `uk_facilities_stack_name` (stack_id, name)
- **INDEX** `idx_facilities_tenant_id` (tenant_id)

---

## preventions — 방지시설

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| prevention_id | BIGINT | PK, AUTO_INCREMENT | |
| tenant_id | BIGINT | NOT NULL, FK→tenants | `fk_preventions_tenants`, ON DELETE CASCADE |
| stack_id | BIGINT | NOT NULL, FK→stacks | `fk_preventions_stacks`, ON DELETE CASCADE |
| name | VARCHAR | NOT NULL | 시설명 |
| capacity | DOUBLE | | 용량 |
| target_name | VARCHAR | | 대상물질명 |
| removal_efficiency | VARCHAR | | 제거효율 |
| created_at / modified_at | DATETIME | | |

- **UNIQUE** `uk_preventions_stack_name` (stack_id, name)
- **INDEX** `idx_preventions_tenant_id` (tenant_id)
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
- 가이드 항목을 시설에 등록하면 이 행이 **자동 생성**됩니다(`PollutantMaterializer`). 이때 `name_kr`만 가이드에서 복사하고
  나머지 고객사 입력값은 비워 둡니다.
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
- 참조 대상은 카탈로그가 아니라 `pollutants`입니다. 클라이언트가 `catalog_id`로 물질을 골라도 서버가 해당 테넌트의 `pollutants` 행을 확보한 뒤 연결합니다(`PollutantMaterializer`).

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

- **UNIQUE** `uk_schedules_stack_team_date` (stack_id, team_id, sampled_at)
- **INDEX** `idx_schedules_tenant_id` (tenant_id)

> 시료접수·분석완료·성적서발행 일자와 채취 시작/종료 시각은 MySQL이 아니라
> MongoDB `schedule_documents.basicInfo`에 있습니다. MySQL의 날짜 컬럼은 `sampled_at` 하나뿐입니다.

### 상태(`ScheduleStatus`) 전이

전이 규칙은 `ScheduleStatus.canTransitionTo()`가 강제합니다. 단계를 건너뛸 수 없고, 되돌릴 수 없습니다.

```
SCHEDULED ──> MEASURING ──> ANALYZING ──> COMPLETED  (종단)
     └─────────────┴─────────────┴───────> CANCELED  (종단)
```

전이 트리거는 **"전진은 자동, 종료는 수동"** 원칙을 따릅니다. 상태는 사용자의 의도가 아니라
업무의 사실을 기록해야 하므로, 사실 신호가 있는 전진은 서버가 자동으로 처리합니다.

| 전이 | 트리거 | 처리 위치 |
|------|--------|-----------|
| `SCHEDULED → MEASURING` | 측정 시트가 처음 저장될 때(시트가 1개 이상) | `ScheduleService.saveSheets` 자동 |
| `MEASURING → ANALYZING` | 시료접수일자(`basicInfo.receivedAt`)가 처음 입력될 때 | `ScheduleService.updateBasicInfo` 자동 |
| `ANALYZING → COMPLETED` | 사용자 확정 | `PATCH /api/schedules/{id}/status` |
| `* → CANCELED` | 사용자 확정 | `PATCH /api/schedules/{id}/status` |

- `COMPLETED`·`CANCELED`는 종단 상태이며 `canEdit() == false`입니다. 이후 모든 수정·삭제 경로가
  `Schedule.requireEditable()`에서 409(`SCHEDULE_NOT_EDITABLE`)로 차단됩니다.
- 측정 건수 통계(`countCompleted` 등)는 `COMPLETED`이면서 `sampled_at`이 있는 건만 집계합니다.
- `status`는 MongoDB 문서에도 사본으로 보관되며(`syncStatus`), 응답·통계는 항상 MySQL 값을 씁니다.

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
| `ScheduleStatus` | SCHEDULED(측정예정), MEASURING(측정중), ANALYZING(분석중), COMPLETED(완료), CANCELED(취소) |

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
