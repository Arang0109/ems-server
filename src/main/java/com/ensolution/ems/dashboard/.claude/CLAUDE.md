# dashboard 모듈 가이드라인

프론트엔드 대시보드(`ems-web`의 `pages/dashboard`)에 통계 자료를 송신하는 **조회 전용 조립 모듈**입니다.

## 성격
- **자체 도메인 원장이 없습니다.** `domain/`·`infrastructure/`·`application/port/out/`을 두지 않습니다.
- 데이터는 전부 **공급 모듈의 인바운드 포트(`application/port/in`)만** 소비해 조립합니다. 타 모듈의 JPA/Repository를 직접 참조하지 않습니다(`ScheduleSnapshotAssembler` 선례와 동일한 크로스모듈 규칙).
- 현재 공급원: `tenant`(`WorkplaceQueryUseCase.countWorkplaces`, `StackQueryUseCase.countStacks`), `schedule`(`ScheduleStatisticsUseCase`).

## 통계 정의 (도메인 규칙)
- **"측정 건수" = `ScheduleStatus.COMPLETED` 상태만** 집계합니다(진행중·CANCELED 제외). 이 규칙은 데이터 소유 모듈인 `schedule`이 `ScheduleStatisticsUseCase`로 소유합니다.
- **기준 날짜 = `Schedule.measureDate`**(측정 실시/예정일). `createdAt` 아님.
- **월별 추이 = 당해 연도 1~12월 고정**, 데이터 없는 달은 `count: 0`.
- `thisMonthMeasurements` = measureDate가 이번 달이면서 COMPLETED인 건수(`YearMonth.now()`).

## 엔드포인트
- `GET /api/dashboard/summary` → `DashboardOverviewResponse` (사업장 수, 측정시설 수, 총·이번달 측정 건수)
- `GET /api/dashboard/measurement-stats` → `List<MeasurementCountChartResponse>` (label "N월", count)

## 규칙
- tenantId는 `@AuthenticationPrincipal CustomUserDetails`에서만 획득합니다(path/body 금지).
- 응답은 `global/web/ApiResponse<T>`.
- VO는 `application/command/`(record), Response는 `presentation/response/`, 매핑은 `presentation/mapper/DashboardMapper`(MapStruct)에 위임합니다.

## 향후 과제
- 집계 데이터가 급증하면 각 공급 모듈 `port/out` 어댑터에 GROUP BY 집계 쿼리를 추가해 전건 로드(`findAll`)를 대체합니다(현재는 규모상 findAll 재사용).
