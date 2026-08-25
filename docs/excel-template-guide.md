# 엑셀 템플릿(jxls) 작성 가이드

> 대상: 성적서·채취기록부 엑셀 양식을 직접 관리하는 고객사 담당자
> 적용 버전: jxls-poi 3.1 / 문서 개정일 2026-08-21

EMS는 **고객사가 사용하던 엑셀 양식(.xlsx)을 그대로 업로드**하면, 그 양식의 셀에 측정 데이터를 채워 돌려줍니다.
양식에 표시할 값을 `${변수}` 형태로 적어 두는 것이 전부이며, 셀 병합·테두리·수식·인쇄 설정 등 기존 서식은 그대로 유지됩니다.

---

## 1. 두 가지 내보내기

| 구분 | 결과물 | 템플릿이 담당하는 범위 |
|------|--------|------------------------|
| **성적서 발행** | 엑셀 파일 1개(.xlsx) | 측정계획 원장 + **전체 측정 시트** |
| **채취기록부 다운로드** | ZIP 1개(측정 시트 수만큼 .xlsx 포함) | **측정 시트 1장**을 그리는 양식. 시스템이 시트 수만큼 반복 생성 |

채취기록부의 ZIP 안 파일명은 `1_먼지.xlsx`, `2_가스상.xlsx` 처럼 `{순번}_{측정 카테고리}.xlsx` 로 만들어집니다.

> **핵심 차이**
> 성적서 템플릿은 시트 목록(`sheets`)을 직접 반복해야 하지만,
> 채취기록부 템플릿은 반복을 신경 쓰지 않고 **"한 장짜리 양식"** 으로 만들면 됩니다. 반복은 시스템이 대신합니다.

---

## 2. 기본 문법 3가지

### 2-1. 값 넣기 — `${변수}`

셀에 아래처럼 입력합니다.

```
${plan.clientName}
```

한 셀에 텍스트와 섞어 쓸 수도 있습니다.

```
측정일: ${plan.sampledAt} (측정자 ${plan.mentor})
```

점(`.`)으로 하위 값을 계속 파고들 수 있습니다.

```
${plan.sheets[0].flow.velocity}
```

### 2-2. 채울 영역 지정 — `jx:area` (필수)

**첫 시트의 A1 셀에 "메모(주석)"를 달고** 아래 내용을 적습니다. 이 메모가 없으면 아무 값도 채워지지 않습니다.

```
jx:area(lastCell="H60")
```

- `lastCell` 은 데이터가 채워질 **영역의 우측 하단 셀**입니다. 양식보다 넉넉하게 잡아 두세요.
- 메모는 결과 파일에서 자동으로 제거됩니다.

> 엑셀에서 메모 다는 법: 셀 우클릭 → **메모 삽입**(또는 새 노트) → 위 문구 입력.
> 메모 안의 작성자 이름(`홍길동:`)은 지워도 되고 남겨도 동작합니다.

### 2-3. 반복 출력 — `jx:each`

목록(측정점, 배출시설, 장비 등)을 행 단위로 반복하려면, **반복할 첫 행의 A열 셀에 메모**를 답니다.

```
jx:each(items="points" var="p" lastCell="H12")
```

| 속성 | 의미 |
|------|------|
| `items` | 반복할 목록 변수명 (예: `points`, `plan.facilities`) |
| `var` | 한 건을 가리킬 이름. 셀에서는 `${p.temperature}` 처럼 사용 |
| `lastCell` | **반복되는 한 덩어리**의 우측 하단 셀 (1행짜리면 그 행의 마지막 열) |
| `direction` | `DOWN`(기본, 아래로) / `RIGHT`(오른쪽으로 열 반복) |

예) 측정점 표를 5행부터 아래로 반복

- A5 셀 메모: `jx:each(items="points" var="p" lastCell="F5")`
- A5: `${p.index}` / B5: `${p.temperature}` / C5: `${p.dynamicPressure}` …

가로로 반복하려면(측정점을 열 방향으로 나열하는 양식):

```
jx:each(items="points" var="p" lastCell="C10" direction="RIGHT")
```

---

## 3. 템플릿에서 쓸 수 있는 최상위 변수

### 성적서 템플릿

| 변수 | 내용 |
|------|------|
| `plan` | 측정계획 원장 전체 (§4) |
| `sheets` | 측정 시트 목록. `plan.sheets` 와 같습니다 |
| `items` | 측정항목 목록 (§4-9). `plan.items` 와 같습니다 |

```
jx:each(items="sheets" var="s" lastCell="H30")
   → ${s.category}, ${s.flow.velocity}, ${s.moisture.ratio} …
```

### 채취기록부 템플릿

시트 1장 기준이라, 자주 쓰는 값은 **짧은 이름으로도** 바로 쓸 수 있습니다.

| 변수 | 내용 | 같은 값의 긴 경로 |
|------|------|-------------------|
| `plan` | 측정계획 원장 전체 (§4) | — |
| `sheet` | 이 파일이 담당하는 측정 시트 | — |
| `weather` | 날씨 (§5-1) | `sheet.weather` |
| `moisture` | 수분 (§5-2) | `sheet.moisture` |
| `gas` | 배출가스 분석 (§5-3) | `sheet.gas` |
| `flow` | 유량 (§5-4) | `sheet.flow` |
| `particle` | 입자상 채취 집계 (§5-5) | `sheet.particle` |
| `points` | 측정점 목록 (§5-6) | `sheet.points` |
| `samples` | 시료 채취 목록 (§5-7) | `sheet.samples` |
| `items` | 측정항목 목록 (§4-9) | `plan.items` |

```
${plan.workplaceName} / ${sheet.category} / ${moisture.ratio} / ${flow.standardQuantity}
```

---

## 4. `plan` — 측정계획 원장

### 4-1. 기본 정보

| 변수 | 내용 | 형식 |
|------|------|------|
| `plan.referenceNumber` | 접수번호(내부 식별 코드) | 문자 |
| `plan.measurementField` | 측정분야 | 대기 / 수질 / 소음진동 / 악취 |
| `plan.schedulePurpose` | 측정용도 | 문자 |
| `plan.sampledAt` | 채취일자 | 날짜 |
| `plan.receivedAt` | 시료접수일자 | 날짜 |
| `plan.analyzedAt` | 분석완료일자 | 날짜 |
| `plan.issuedAt` | 성적서발행일자 | 날짜 |
| `plan.samplingStartedAt` | 채취 시작시각 | 시각 |
| `plan.samplingEndedAt` | 채취 종료시각 | 시각 |

### 4-2. 담당자

| 변수 | 내용 |
|------|------|
| `plan.mentor` | 측정자(정) |
| `plan.mentee` | 측정자(부) |
| `plan.facilityManager` | 배출시설관리자 |
| `plan.samplingWitness` | 시료채취 입회자(환경기술인) |
| `plan.analyst` | 시료분석 검사자 |
| `plan.technicalManager` | 기술책임자 |

### 4-3. 측정대행업체(고객사) / 의뢰기관

| 측정대행업체 | 의뢰기관 | 내용 |
|--------------|----------|------|
| `plan.tenantName` | `plan.clientName` | 상호명 |
| `plan.tenantBizNumber` | `plan.clientBizNumber` | 사업자등록번호 |
| `plan.tenantRepresentative` | `plan.clientRepresentative` | 대표자 |
| `plan.tenantAddress` | `plan.clientAddress` | 주소(도로명 + 상세) |

### 4-4. 사업장

| 변수 | 내용 |
|------|------|
| `plan.workplaceName` | 사업장명 |
| `plan.workplaceBizNumber` | 사업자등록번호 |
| `plan.businessCategory` | 업종 |
| `plan.workplaceAddress` | 사업장 주소 |
| `plan.workplaceGrade` | 사업장 종별 (1종~5종) |

### 4-5. 측정시설(굴뚝)

| 변수 | 내용 | 형식 |
|------|------|------|
| `plan.stackName` | 배출구명 | 문자 |
| `plan.semsNumber` | SEMS 번호 | 문자 |
| `plan.mainProduct` | 주 생산품 | 문자 |
| `plan.stackGrade` | 배출구 종별 (1종~5종) | 문자 |
| `plan.stackShape` | 단면 형태 | 원형 / 사각형 |
| `plan.stackOrientation` | 배출구 방향 | 수직 / 수평 |
| `plan.horizontalLength` | 가로 길이(원형이면 직경) | 숫자 |
| `plan.verticalLength` | 세로 길이 | 숫자 |
| `plan.height` | 높이 | 숫자 |
| `plan.standardOxygen` | 기준산소농도 (%) | 정수 |

### 4-6. 배출시설 `plan.facilities` (반복)

`jx:each(items="plan.facilities" var="f" lastCell="G10")`

| 변수 | 내용 |
|------|------|
| `f.name` | 시설명 |
| `f.fuelUsage` | 연료 사용량 |
| `f.productOutput` | 제품 생산량 |
| `f.incinerationAmount` | 소각량 |
| `f.fuelInput` | 원료 투입량 |
| `f.fuelType` | 연료·원료 종류 |
| `f.unit` | 사용량·생산량 단위 |

### 4-7. 방지시설 `plan.preventions` (반복)

`jx:each(items="plan.preventions" var="pv" lastCell="E10")`

| 변수 | 내용 |
|------|------|
| `pv.name` | 시설명 |
| `pv.capacity` | 용량 |
| `pv.unit` | 용량 단위 |
| `pv.targetName` | 대상 물질명 |
| `pv.removalEfficiency` | 제거효율 |

### 4-8. 측정 장비

**슬롯 단건 참조** — 그 측정에 사용한 장비를 바로 지목합니다.

| 변수 | 장비 |
|------|------|
| `plan.particleSampler` | 입자상 시료채취장비 |
| `plan.gasSampler` | 가스상 시료채취장비 |
| `plan.pitotTube` | 피토관 |
| `plan.nozzle` | 노즐 |

```
${plan.particleSampler.managementNumber} / ${plan.pitotTube.modelName}
```

**전체 목록 반복** — `plan.equipments`

`jx:each(items="plan.equipments" var="e" lastCell="H20")`

| 변수 | 내용 |
|------|------|
| `e.type` | 장비 유형 코드 (§6) |
| `e.typeLabel` | 장비 유형 한글명 |
| `e.managementNumber` | 관리번호 |
| `e.serialNumber` | 제조번호 |
| `e.modelName` | 모델명 |
| `e.equipmentName` | 장비명 |
| `e.alias` | 별칭 |
| `e.manufacturer` | 제조사 |
| `e.calibrationCycle` | 교정 주기(개월) |
| `e.lastCalibrationDate` | 최종 교정일 |
| `e.calibrationDueDate` | 다음 교정 예정일 |
| `e.totalVolume` | 채취기 총 용량 (채취기 유형만) |
| `e.orificeDeltaH` | 오리피스 보정계수 △H@ (입자상 채취기) |
| `e.yd` | 건식가스미터 계수 Yd (입자상 채취기) |
| `e.pitotTubeType` / `e.pitotTubeTypeLabel` | 피토관 유형 코드 / 한글명 |

**장비의 하위 목록** (장비 반복 안에서 다시 반복하거나, 슬롯 변수에서 바로 반복)

| 목록 | 항목 변수 |
|------|-----------|
| `e.inspections` — 검사 이력 | `type`, `typeLabel`(정도검사/교정/일반시험), `enabled`(검사 대상 여부), `cycleMonths`(주기·개월), `lastInspectedAt`(최종 수검일), `nextDueDate`(다음 예정일) |
| `e.coefficients` — 피토관 계수표 | `velocity`(적용 유속 하한 m/s), `coefficient`(계수 Cp) |
| `e.nozzleDiameters` — 보유 노즐경 | 값 자체가 숫자이므로 `${d}` 처럼 그대로 사용 |

```
jx:each(items="plan.pitotTube.coefficients" var="c" lastCell="C15")
   → ${c.velocity} , ${c.coefficient}
```

### 4-9. 측정항목 `items`

이번 계획에서 측정한 항목 목록입니다. **성적서·채취기록부 양쪽에서 `items` 라는 짧은 이름으로 바로 쓸 수 있고**,
`plan.items` 로도 같은 값을 가리킵니다.

| 변수 | 내용 |
|------|------|
| `items[n].name` | 측정항목명(국문) |
| `items[n].nameEn` | 측정항목명(영문) |
| `items[n].code` | 측정물질 코드(예: `NOX`). 예전에 만든 계획은 비어 있을 수 있습니다 |
| `items[n].allowance` | 배출허용기준. 미지정이면 빈칸 |
| `items[n].oxygenApplicable` | 기준산소농도 적용 여부 (true/false) |
| `items[n].cycle` | 측정주기 |
| `items[n].equipment` | 시험장비 |
| `items[n].testMethod` | 시험방법 |

**순서는 화면에서 정합니다.** 측정계획 상세의 **성적서 탭**에서 항목을 끌어 옮긴 순서가 그대로
`items[0]`, `items[1]` … 이 됩니다.

#### 항목 수가 서식 한 장을 넘칠 때 — 시트를 미리 여러 장 만들어 둡니다

대기측정기록부처럼 **한 장에 실을 수 있는 항목 수가 정해진 서식**(4개)은,
양식 파일에 기록부 시트를 필요한 만큼 복사해 두고 시트마다 인덱스를 이어서 적습니다.

```
[기록부 1장] B31=${items[0].name}  B32=${items[1].name}  B33=${items[2].name}  B34=${items[3].name}
[기록부 2장] B31=${items[4].name}  B32=${items[5].name}  B33=${items[6].name}  B34=${items[7].name}
[기록부 3장] B31=${items[8].name}  …
```

- 항목이 7개면 2장, 9개면 3장이 채워지고, **남는 칸은 빈칸으로 남습니다.**
- 그러니 **쓸 수 있는 최대 항목 수만큼 시트를 미리 만들어 두면 됩니다.**
  다 쓰이지 않은 시트는 항목 칸이 비어 있는 채로 출력되므로, 인쇄 전에 지우거나 숨기면 됩니다.
- `jx:each` 로 반복하지 않고 **인덱스를 직접 적는 방식**입니다. 서식의 칸 위치가 고정되어 있어
  반복보다 이 편이 안전합니다.

> 항목명(`name`)을 다른 시트에서 `INDEX`/`MATCH` 로 찾아 쓰는 양식이라면, 찾는 쪽 목록의 표기와
> 항목명이 **글자까지 똑같아야** 합니다. 표기가 다르면 수식이 값을 찾지 못합니다.

---

## 5. 측정 시트 값

`sheet` 자체의 값:

| 변수 | 내용 |
|------|------|
| `sheet.category` | 측정 카테고리 — 가스상 / 중금속 / 먼지 / 수은 |
| `sheet.pointCount` | 규정상 요구 측정점 수 |

> 아래 표의 `weather.*` 는 **채취기록부** 기준 표기입니다.
> 성적서에서는 `s.weather.*`(시트 반복 변수) 또는 `plan.sheets[0].weather.*` 로 쓰세요.

### 5-1. 날씨 `weather`

| 변수 | 내용 | 단위 |
|------|------|------|
| `weather.pressureHpa` | 대기압(입력값) | hPa |
| `weather.pressureMmHg` | 대기압(환산값) | mmHg |
| `weather.condition` | 날씨 | 맑음/흐림/비/눈 |
| `weather.temperature` | 외기온도 | ℃ |
| `weather.humidity` | 상대습도 | % |
| `weather.windDirection` | 풍향 | 정온/북/북북동 … |
| `weather.windSpeed` | 풍속 | m/s |

### 5-2. 수분 `moisture`

**입력값**

| 변수 | 내용 | 단위 |
|------|------|------|
| `moisture.weightBefore` / `moisture.weightAfter` | 흡습병 무게 전 / 후 | g |
| `moisture.inTemperature` / `moisture.outTemperature` | 가스미터 입구 / 출구 온도 | ℃ |
| `moisture.volumeBefore` / `moisture.volumeAfter` | 건조가스 적산 부피 전 / 후 | L |
| `moisture.suctionVelocity` | 흡입속도 | |
| `moisture.gaugePressureMmH2O` | 가스미터 게이지압(입력값) | mmH2O |
| `moisture.startTime` / `moisture.endTime` | 채취 시작 / 종료 시각 | 시각 |

**계산값**

| 변수 | 내용 | 단위 |
|------|------|------|
| `moisture.ratio` | 수분량 Xw | % |
| `moisture.absorbedMass` | 흡습 수분질량 ma | g |
| `moisture.avgTemperature` | 가스미터 흡입가스 평균온도 Tm | ℃ |
| `moisture.dryGasVolume` | 흡입 건조가스량 Vm | L |
| `moisture.gaugePressureMmHg` | 가스미터 게이지압(환산) | mmHg |
| `moisture.gaugePressureInchH2O` | 가스미터 게이지압(환산) | inchH2O |

### 5-3. 배출가스 분석 `gas`

| 변수 | 내용 | 단위 |
|------|------|------|
| `gas.o2` / `gas.co2` / `gas.co` | 측정점별 O2 / CO2 / CO 농도 **목록** | vol % |
| `gas.nox` / `gas.sox` | 측정점별 NOx / SOx 농도 **목록** | ppm |
| `gas.analyzerStartTime` | 가스분석기 측정 시작시각 | 시각 |
| `gas.thcStartTime` | THC분석기 측정 시작시각 | 시각 |
| `gas.standardDensity` | 표준상태 습윤 배출가스 밀도 | kg/Sm³ |
| `gas.o2CorrectionFactor` | 산소보정계수 | — |

농도는 **측정점 순서대로 들어있는 목록**입니다. 반복하거나 순번으로 지목합니다.

```
jx:each(items="gas.o2" var="v" lastCell="B8")   →  ${v}
또는 특정 측정점만:  ${gas.o2[0]}   (0 = 첫 번째 측정점)
```

### 5-4. 유량 `flow`

| 변수 | 내용 | 단위 |
|------|------|------|
| `flow.area` | 측정시설 단면적 | m² |
| `flow.avgTemperature` | 평균 배출가스 온도 | ℃ |
| `flow.avgTemperatureK` | 평균 배출가스 절대온도 | K |
| `flow.avgDynamicPressure` | 평균 동압 Pv | mmH2O |
| `flow.avgStaticPressure` | 평균 정압 Ps | mmH2O |
| `flow.density` | 현장 배출가스 밀도 | kg/m³ |
| `flow.pitotCoefficient` | 피토관 계수 Cp | — |
| `flow.velocity` | 평균 유속 Vs | m/s |
| `flow.quantity` | 현장 습윤 유량 | m³/h |
| `flow.standardQuantity` | 표준상태 건조 유량 | Sm³/h |

### 5-5. 입자상 채취 집계 `particle`

> 먼지·중금속·수은 시트에서만 값이 채워집니다. 가스상 시트에서는 빈칸으로 출력됩니다.

| 변수 | 내용 | 단위 |
|------|------|------|
| `particle.thimbleFilter` | 원통여지 번호 | |
| `particle.blankThimbleFilter` | 바탕(공시료) 원통여지 번호 | |
| `particle.startTime` / `particle.endTime` | 채취 시작 / 종료 시각 | 시각 |
| `particle.avgKFactor` | 평균 K계수 | — |
| `particle.avgOrificePressure` | 평균 오리피스 차압 | mmH2O |
| `particle.avgIsokineticRatio` | 평균 등속흡입계수 | % |
| `particle.totalDryGasVolume` | 총 건식가스미터 채취량 | m³ |
| `particle.totalSamplingTime` | 총 채취시간 | min |
| `particle.avgMeterTemperatureK` | 가스미터 평균 절대온도 | K |

### 5-6. 측정점 목록 `points` (반복)

`jx:each(items="points" var="p" lastCell="M12")`

**유량 입력·계산**

| 변수 | 내용 | 단위 |
|------|------|------|
| `p.index` | 측정점 번호(1부터) | |
| `p.temperature` | 배출가스 온도 Ts | ℃ |
| `p.dynamicPressure` | 동압 Pv | mmH2O |
| `p.staticPressure` | 정압 Ps | mmH2O |
| `p.velocity` | 유속 Vs | m/s |
| `p.density` | 측정점 배출가스 밀도 | kg/m³ |

**입자상 입력·계산** (입자상 측정점에서만 채워짐)

| 변수 | 내용 | 단위 |
|------|------|------|
| `p.nozzleSize` | 노즐 직경 | cm |
| `p.samplingTime` | 채취 시간 | min |
| `p.vacuumPressure` | 진공게이지 압력 | |
| `p.impingerTemperature` | 최종 임핀저 온도 | ℃ |
| `p.inTemperature` / `p.outTemperature` | 가스미터 입구 / 출구 온도 | ℃ |
| `p.volumeBefore` / `p.volumeAfter` | 건식가스미터 채취 전 / 후 적산값 | m³ |
| `p.avgTemperature` | 가스미터 평균온도 | ℃ |
| `p.dryGasVolume` | 건식가스미터 채취량 Vm | m³ |
| `p.collectedWater` | 채취된 물의 총량 Vlc | mL |
| `p.kFactor` | K계수 | — |
| `p.orificePressure` | 오리피스 차압 | mmH2O |
| `p.isokineticRatio` | 등속흡입계수 | % |

### 5-7. 시료 채취 목록 `samples` (반복)

`jx:each(items="samples" var="sm" lastCell="L10")`

| 변수 | 내용 | 단위 |
|------|------|------|
| `sm.name` | 시료명 | |
| `sm.number` | 시료 번호 | |
| `sm.blankNumber` | 공시료 번호 | |
| `sm.startTime` / `sm.endTime` | 채취 시작 / 종료 시각 | 시각 |
| `sm.suctionQuantity` | 흡입량 | |
| `sm.gaugePressure` | 가스미터 게이지압 | mmH2O |
| `sm.inTemperature` / `sm.outTemperature` | 가스미터 입구 / 출구 온도 | ℃ |
| `sm.volumeBefore` / `sm.volumeAfter` | 채취 전 / 후 부피 | L |
| `sm.samplingVolume` | 채취 부피 | L |

---

## 6. 코드값 대조표

`typeLabel` 계열 변수를 쓰면 한글명이 바로 출력됩니다. 조건 분기 등에 코드값이 필요할 때 참고하세요.

| 구분 | 코드 → 표기 |
|------|-------------|
| 측정분야 | `AIR` 대기 / `WATER` 수질 / `NOISE_VIBRATION` 소음진동 / `ODOR` 악취 |
| 측정 카테고리 | `GAS` 가스상 / `HEAVY_METAL` 중금속 / `DUST` 먼지 / `MERCURY` 수은 |
| 종별 | `TYPE_1`~`TYPE_5` → 1종~5종 |
| 장비 유형 | `PARTICLE_SAMPLER` 입자상 시료채취장비 / `GAS_SAMPLER` 가스상 시료채취장비 / `GAS_ANALYZER` 배출가스 분석기 / `PITOT_TUBE` 피토관 / `NOZZLE` 노즐 / `OTHER` 기타 |
| 검사 종류 | `PRECISION_INSPECTION` 정도검사 / `CALIBRATION` 교정 / `GENERAL_TEST` 일반시험 |
| 피토관 유형 | `DUST` 먼지 / `FINE_DUST` 미세먼지 / `MERCURY` 수은 |
| 날씨 | 맑음 / 흐림 / 비 / 눈 |
| 풍향 | 정온, 북, 북북동, 북동, 동북동, 동, 동남동, 남동, 남남동, 남, 남남서, 남서, 서남서, 서, 서북서, 북서, 북북서 |
| 단면 형태 / 방향 | 원형·사각형 / 수직·수평 |

> 측정분야·종별·측정 카테고리 등은 `plan.measurementField`, `plan.workplaceGrade`, `sheet.category` 처럼
> **이미 한글로 변환된 값**이 들어옵니다. 위 코드는 참고용입니다.

---

## 7. 실무 팁

### 조건부 출력 — `jx:if`

특정 조건일 때만 영역을 출력합니다. 조건이 걸릴 영역 첫 셀에 메모를 답니다.

```
jx:if(condition="sheet.category == '먼지'" lastCell="H20" areas=["A10:H20","A22:H32"])
```

`areas` 의 첫 번째가 조건이 참일 때, 두 번째가 거짓일 때 사용할 영역입니다.

### 날짜·시각 표시 형식

날짜·시각은 셀에 값으로 채워집니다. **표시 형식은 템플릿 셀의 서식**(셀 서식 → 날짜/시간)으로 지정하세요.
문자열로 직접 조립할 수도 있습니다.

```
${plan.sampledAt.year}년 ${plan.sampledAt.monthValue}월 ${plan.sampledAt.dayOfMonth}일
```

### 엑셀 수식

템플릿에 넣어 둔 수식은 그대로 유지되며, **파일을 열 때 자동으로 재계산**됩니다.
따라서 미리보기 도구(웹 뷰어 등)로 결과를 열면 수식 셀이 비어 보일 수 있습니다 — 엑셀에서 열면 정상 계산됩니다.

수식 범위가 반복 행을 따라 늘어나야 한다면 jxls 수식 표기를 사용하세요.

```
$[SUM(F5)]     ← 반복 영역과 함께 범위가 자동 확장됩니다
```

### 값이 없을 때

데이터가 없는 항목은 **빈칸**으로 출력됩니다. 오류가 나지 않으니 안심하고 배치해도 됩니다.
단, 이 문서에 **없는 이름**을 쓰면 빈칸이 아니라 내보내기 실패로 이어집니다.

---

## 8. 오류가 났을 때

> `엑셀 템플릿 처리에 실패했습니다. 템플릿의 jxls 문법을 확인해 주세요.`

이 메시지가 뜨면 아래를 순서대로 확인하세요.

1. **A1 셀에 `jx:area` 메모가 있는지** — 가장 흔한 원인입니다.
2. **변수명 오타** — 대소문자를 구분합니다. `plan.clientname`(X) / `plan.clientName`(O).
3. **`lastCell` 범위** — 반복 영역의 `lastCell` 이 실제 양식 범위보다 작으면 일부 셀이 누락되고, 겹치면 결과가 밀립니다.
4. **`jx:each` 의 `items` 가 목록인지** — 목록이 아닌 값(`flow.velocity` 등)에 `jx:each` 를 걸면 실패합니다.
5. **메모 위치** — `jx:each` 메모는 반복 영역의 **좌측 상단 셀**에 달아야 합니다.
6. **파일 형식** — `.xlsx` 만 지원합니다(`.xls`, `.xlsm` 불가).
7. **채취기록부** 는 측정 시트가 한 장도 없는 측정계획에서는 내보낼 수 없습니다.

그래도 해결되지 않으면 **업로드한 템플릿 파일과 오류 발생 시각**을 담당자에게 전달해 주세요.

---

## 9. 최소 예제 — 채취기록부 한 장

| | A | B | C | D |
|---|---|---|---|---|
| **1** | `${plan.workplaceName}`<br>※메모: `jx:area(lastCell="D12")` | | `${plan.stackName}` | `${sheet.category}` |
| **2** | 채취일자 | `${plan.sampledAt}` | 측정자 | `${plan.mentor}` |
| **3** | 대기압(mmHg) | `${weather.pressureMmHg}` | 외기온도(℃) | `${weather.temperature}` |
| **4** | 수분량(%) | `${moisture.ratio}` | 평균유속(m/s) | `${flow.velocity}` |
| **5** | 측정점 | 온도(℃) | 동압(mmH2O) | 유속(m/s) |
| **6** | `${p.index}`<br>※메모: `jx:each(items="points" var="p" lastCell="D6")` | `${p.temperature}` | `${p.dynamicPressure}` | `${p.velocity}` |

6행이 측정점 수만큼 아래로 늘어나고, 이 파일이 측정 시트 수만큼 만들어져 ZIP으로 묶입니다.
