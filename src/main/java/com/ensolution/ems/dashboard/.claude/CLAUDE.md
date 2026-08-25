# dashboard 모듈 가이드라인

프론트엔드 대시보드(`ems-web`의 `pages/dashboard`)에 통계 자료를 송신하는 **조회 전용 조립 모듈**입니다.

## 성격
- **자체 도메인 원장이 없습니다.** `domain/`·`infrastructure/`·`application/port/out/`을 두지 않습니다.
- 데이터는 전부 **공급 모듈의 인바운드 포트(`application/port/in`)만** 소비해 조립합니다. 타 모듈의 JPA/Repository를 직접 참조하지 않습니다(`ScheduleSnapshotAssembler` 선례와 동일한 크로스모듈 규칙).
- 현재 공급원: `client_management`(`WorkplaceQueryUseCase.countWorkplaces`, `StackQueryUseCase.countStacks`), `schedule`(`ScheduleStatisticsUseCase`), `contract`(`ContractStatisticsUseCase`), `equipment`(`EquipmentQueryUseCase.findInspectionDueBefore`).
- 공급 모듈의 `port/in` VO → 대시보드 VO 변환은 **인터모듈 매퍼** `application/mapper/`에 둡니다(`ContractPortMapper`, `EquipmentPortMapper`). 표시용 파생값(잔여일수 등)은 이 계층에서 계산합니다.

## 통계 정의 (도메인 규칙)
- **"측정 건수" = `ScheduleStatus.REPORT_COMPLETED` 상태만** 집계합니다(진행중·CANCELED 제외). 이 규칙은 데이터 소유 모듈인 `schedule`이 `ScheduleStatisticsUseCase`로 소유합니다.
- **기준 날짜 = `Schedule.measureDate`**(측정 실시/예정일). `createdAt` 아님.
- **월별 추이 = 당해 연도 1~12월 고정**, 데이터 없는 달은 `count: 0`.
- `thisMonthMeasurementCount` = measureDate가 이번 달이면서 COMPLETED인 건수(`YearMonth.now()`).
- **"계약 만료 임박" = `Contract.completionDate`가 오늘 ~ 오늘+2개월** 구간인 계약. 이미 만료된 건(오늘 이전)과 `completionDate`가 없는 계약은 제외하며, **임박한 순(완료일 오름차순)** 으로 정렬합니다. 계약에는 status 필드가 없으므로 완료일이 유일한 만료 기준입니다.
  - "완료일이 만료 기준"이라는 규칙은 소유 모듈인 `contract`가 `ContractStatisticsUseCase`로 소유하고, **몇 개월 이내를 임박으로 볼지는 대시보드의 표시 정책**이므로 `DashboardService.EXPIRY_WITHIN_MONTHS`가 구간을 정해 넘깁니다.
  - `daysRemaining`은 조회 기준일(오늘)부터 완료일까지의 일수로, 위 범위 정의상 항상 0 이상입니다.
- **"신규 계약" = `Contract.contractDate`가 당월**인 계약 수(`newContractCount`).
- **"검사 임박" = 다음 검사 예정일이 오늘+2개월 이내**인 **ACTIVE 장비의 검사 항목**. 계약과 달리 **기한이 지난 항목도 포함**하며(검사 기한 초과는 측정 신뢰성에 직결되므로 가장 먼저 보여야 함), 이 경우 `daysRemaining`이 음수입니다. **검사 예정일 순(오름차순)** 으로 정렬하므로 기한 초과 항목이 목록 앞에 옵니다.
  - **단위가 장비가 아니라 장비-검사항목입니다.** 측정장비는 정도검사·교정·일반시험을 중복해서 받으므로, 한 장비가 최대 3건까지 목록에 나올 수 있습니다(`equipmentId`가 같고 `inspectionType`만 다름). 프론트에서 장비 단위로 묶어 표시할지는 표시 정책입니다.
  - **다음 검사 예정일**: `nextDueDateOverride`(성적서에 적힌 유효기간 만료일)가 있으면 그 값, 없으면 `lastInspectedAt` + `cycleMonths`개월. 이 계산은 소유 모듈인 `equipment`의 `InspectionItem.nextDueDate()`가 소유합니다. 둘 다 특정할 수 없으면 목록에서 제외합니다.
  - **검사 대상이 아닌 종류(`enabled=false`)와 알림을 끈 종류(`notificationEnabled=false`)는 제외**합니다. 두 플래그는 의미가 다릅니다 — 전자는 "이 장비는 그 검사를 받지 않는다", 후자는 "받긴 하지만 대시보드 알림은 원하지 않는다"입니다.
  - INACTIVE(사용 중지)·MAINTENANCE(점검·수리·보정)·DELETED 장비는 제외합니다.

## 엔드포인트
- `GET /api/dashboard/summary` → `DashboardOverviewResponse`
  (사업장·계약·측정시설 수, 총·이번달 측정 건수, 이번달 신규 계약 수, 만료 임박 계약 목록, 검사 임박 장비 목록)
- `GET /api/dashboard/measurement-stats` → `List<MeasurementCountChartResponse>` (label "N월", count)

## 규칙
- tenantId는 `@AuthenticationPrincipal CustomUserDetails`에서만 획득합니다(path/body 금지).
- 응답은 `global/web/ApiResponse<T>`.
- VO는 `application/command/`(record), Response는 `presentation/response/`, 매핑은 `presentation/mapper/DashboardMapper`(MapStruct)에 위임합니다.
- `DashboardOverview`(VO)와 `DashboardOverviewResponse`는 **필드명을 1:1로 일치**시킵니다. MapStruct가 이름 기준으로 매핑하므로, 한쪽만 확장하면 조용히 0/null이 응답으로 나갑니다.
  - 이 실수를 컴파일 오류로 승격시키기 위해 `DashboardMapper`에 `unmappedTargetPolicy = ReportingPolicy.ERROR`를 걸어 두었습니다. 응답에 필드를 추가했는데 VO에 없으면 빌드가 깨집니다.

## 향후 과제
- 집계 데이터가 급증하면 각 공급 모듈 `port/out` 어댑터에 GROUP BY 집계 쿼리를 추가해 전건 로드(`findAll`)를 대체합니다(현재는 규모상 findAll 재사용).
  - 특히 `ContractRepository.findAllByTenantId`는 `ContractTableViewEntity`(`@Subselect` + `GROUP_CONCAT` 서브쿼리) 뷰를 전건 로드하므로, 계약이 늘면 이 경로가 먼저 병목이 됩니다. `completion_date` 인덱스도 아직 없습니다.
  - 검사 임박 조회도 `EquipmentRepository.findAll(tenantId)` 전건 로드 후 스트림 필터입니다. 다음 검사 예정일이 저장 값이 아니라 계산 값이라 Mongo 쿼리로 직접 거를 수 없으므로, 장비가 늘면 예정일을 문서에 비정규화해 저장하거나 aggregation 파이프라인으로 옮겨야 합니다.
- 검사 항목 단위로 나오면서 목록 길이가 장비 수의 최대 3배가 됩니다. 표시 상한(상위 N건)이나 장비 단위 그룹핑이 필요한지 검토해야 합니다.
- 검사 실시는 `POST /api/equipments/{equipmentId}/inspections`로 기록하며, 이 요청이 해당 검사 항목의 최종 수검일을 갱신합니다. 검사 개념 도입 이전에 등록된 장비는 검사 항목이 전부 비활성이라 목록에 잡히지 않으므로, 장비 수정으로 대상 검사를 켜 주어야 합니다.
