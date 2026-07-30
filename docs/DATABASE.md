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
├── pollutants         (측정물질 마스터)   tenant_id
├── stack_pollutant    (시설별 측정물질)   tenant_id, stack_id, pollutant_id
└── contract           (계약)              tenant_id, workplace_id
```

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

## pollutants — 측정물질 마스터

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| pollutant_id | BIGINT | PK, AUTO_INCREMENT | |
| tenant_id | BIGINT | NOT NULL, FK→tenants | `fk_pollutants_tenants`, ON DELETE CASCADE |
| field | VARCHAR | ENUM(String) | `MeasurementField` |
| name_kr | VARCHAR | NOT NULL, UNIQUE | 한글명 |
| name_en | VARCHAR | UNIQUE | 영문명 |
| method | VARCHAR | ENUM(String) | `MeasurementMethod` |
| phase | VARCHAR | ENUM(String) | `PollutantPhase` |
| equipment | VARCHAR | | 장비 |
| test_method | VARCHAR | | 시험방법 |
| created_at / modified_at | DATETIME | | |

- **INDEX** `idx_pollutants_tenant_id` (tenant_id)
- ⚠️ **알려진 이슈**: 엔티티에 클래스 레벨 `@UniqueConstraint uk_pollutant_name_tenant (tenant_id, **name**)`가 선언돼 있으나 `name` 컬럼은 존재하지 않습니다(실제 컬럼은 `name_kr`/`name_en`). DDL 생성 시 오류 소지가 있어 엔티티 수정이 필요합니다.

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
| created_at / modified_at | DATETIME | | |

- **UNIQUE** `uk_tenantId_stackId_pollutantId` (tenant_id, stack_id, pollutant_id)
- **INDEX** `idx_stackPollutant_tenantId` (tenant_id)

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

---

## 참고 / 후속 과제

1. **`pollutants` 유니크 제약 버그**: `uk_pollutant_name_tenant`가 존재하지 않는 `name` 컬럼 참조 → 엔티티 수정 필요.
2. **`contract.contract_amount_unit` ORDINAL 저장**: `@Enumerated(EnumType.STRING)` 부재. enum 순서 변경 시 데이터 깨짐 위험 → STRING 저장 권장.
3. **Auditing 리스너 범위**: `@EntityListeners(AuditingEntityListener.class)`는 `stacks`, `users`에만 부착됨. 나머지 테넌트 테이블은 `created_at`/`modified_at`이 자동 세팅되지 않으므로, 필요 시 각 엔티티에 리스너를 추가하고 `@EnableJpaAuditing` 설정을 확인해야 합니다.
4. **tenant_id 주입 경로**: 인증된 사용자의 `tenant_id`(`users.tenant_id`)를 `CustomUserDetails.tenantId`로 로드하여, 컨트롤러가 `@AuthenticationPrincipal`로 읽어 생성 커맨드에 주입합니다.
5. **`target_substances` 테이블 수동 삭제 필요**: 측정대상물질은 `preventions.target_name`/`removal_efficiency`로 통합되어 엔티티가 제거됐지만, `ddl-auto: update`는 테이블/컬럼 삭제를 반영하지 않습니다. 기존 DB에 남아 있는 `target_substances` 테이블은 `DROP TABLE target_substances;`로 직접 정리해야 합니다.
