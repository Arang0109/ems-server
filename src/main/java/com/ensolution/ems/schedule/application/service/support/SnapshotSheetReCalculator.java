package com.ensolution.ems.schedule.application.service.support;

import com.ensolution.ems.equipment.domain.spec.NozzleSpec;
import com.ensolution.ems.equipment.domain.spec.ParticleSamplerSpec;
import com.ensolution.ems.equipment.domain.spec.PitotTubeSpec;
import com.ensolution.ems.schedule.application.calculation.SamplingItemInput;
import com.ensolution.ems.schedule.application.calculation.SheetCalculator;
import com.ensolution.ems.schedule.application.calculation.StackData;
import com.ensolution.ems.schedule.application.service.ScheduleService;
import com.ensolution.ems.schedule.domain.sampling.MeasurementCategory;
import com.ensolution.ems.schedule.domain.sampling.SamplingSheet;
import com.ensolution.ems.schedule.domain.snapshot.ClientSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.EquipmentSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.StackSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 측정계획 스냅샷의 측정시설·장비 spec을 계산 입력으로 삼아 측정 시트를 재계산한다.
 * 계산 입력 추출(표준산소농도·굴뚝 형상/치수·피토관 계수·노즐경·측정항목)이라는 응집된 책임을 담당해
 * {@link ScheduleService}가 유스케이스 조율에 집중하도록 한다. 계산 엔진은 스냅샷을 모르므로
 * 스냅샷에서 뽑을 것은 전부 여기서 DTO·집합으로 바꿔 넘긴다.
 */
@Component
@RequiredArgsConstructor
public class SnapshotSheetReCalculator {

	private final SheetCalculator sheetCalculator;

	/** 스냅샷의 장비 spec·측정시설 정보를 계산 입력으로 삼아 시트를 재계산한다. */
	public List<SamplingSheet> reCalculate(ScheduleSnapshot snapshot, List<SamplingSheet> sheets) {
		if (sheets == null || sheets.isEmpty()) return sheets;

		StackData stackData = resolveStackData(snapshot);
		if (stackData == null) return sheets;   // 측정시설 스냅샷이 없으면 계산 입력이 없으므로 원본을 유지한다.

		List<PitotTubeSpec.PitotCoefficient> pitotCoefficients = resolvePitotCoefficients(snapshot);
		List<NozzleSpec.NozzleDiameter> nozzleDiameters = resolveNozzleSizes(snapshot);
		BigDecimal deltaH = resolveDeltaH(snapshot);
		List<SamplingItemInput> items = resolveItems(snapshot);

		return sheets.stream()
			.map(sheet -> sheetCalculator.calculate(
				sheet, stackData, pitotCoefficients, nozzleDiameters, deltaH, items))
			.toList();
	}

	/**
	 * 가스상 시료 행의 파생값 계산에 쓰는 측정항목 입력. 등속흡인 항목(카탈로그 측정방식이 먼지·중금속·수은)은
	 * 그 방식의 입자상 기록지 카테고리를 출처로 갖는다({@code MeasurementCategory#particulateSourceOf}) — 그 항목이
	 * 가스상 행에 실리면(비소화합물 흡수액) 그 행은 같은 기록지의 입자상 집계에서 파생된다({@code IsokineticSampleStep}).
	 * 표준 채취시간은 시작시각만 적힌 행의 종료시각 기본값이다({@code SamplingEndTimeStep}).
	 * 측정방식 도입 이전 문서는 mode가 null이라 등속흡인 행이 없다.
	 */
	private List<SamplingItemInput> resolveItems(ScheduleSnapshot snapshot) {
		if (snapshot.items() == null) return List.of();
		return snapshot.items().stream()
			.map(item -> new SamplingItemInput(
				item.pollutantId(),
				MeasurementCategory.particulateSourceOf(item.mode()),
				item.samplingMinutes()))
			.toList();
	}

	/** 오리피스 보정계수(△H@)를 ParticleSampler spec에서 가져온다. 없으면 null(스텝에서 기본값 사용). */
	private BigDecimal resolveDeltaH(ScheduleSnapshot snapshot) {
		if (snapshot.equipments() == null) return null;
		return snapshot.equipments().stream()
			.map(EquipmentSnapshot::spec)
			.filter(spec -> spec instanceof ParticleSamplerSpec)
			.map(spec -> ((ParticleSamplerSpec) spec).orificeDp())
			.filter(orificeDp -> orificeDp != null)
			.findFirst()
			.orElse(null);
	}

	private List<PitotTubeSpec.PitotCoefficient> resolvePitotCoefficients(ScheduleSnapshot snapshot) {
		if (snapshot.equipments() == null) return null;
		return snapshot.equipments().stream()
			.map(EquipmentSnapshot::spec)
			.filter(spec -> spec instanceof PitotTubeSpec)
			.map(spec -> ((PitotTubeSpec) spec).coefficients())
			.filter(coefficients -> coefficients != null)
			.findFirst()
			.orElse(null);
	}

	private List<NozzleSpec.NozzleDiameter> resolveNozzleSizes(ScheduleSnapshot snapshot) {
		if (snapshot.equipments() == null) return null;
		return snapshot.equipments().stream()
			.map(EquipmentSnapshot::spec)
			.filter(spec -> spec instanceof NozzleSpec)
			.map(spec -> ((NozzleSpec) spec).diameters())
			.filter(diameters -> diameters != null)
			.findFirst()
			.orElse(null);
	}

	/** 계산 입력(표준산소농도·굴뚝 형상/치수/방향)을 측정시설 스냅샷에서 가져온다. */
	private StackData resolveStackData(ScheduleSnapshot snapshot) {
		ClientSnapshot client = snapshot.client();
		if (client == null || client.workplace() == null || client.workplace().stack() == null) return null;
		StackSnapshot stack = client.workplace().stack();
		return new StackData(
			stack.shape(),
			stack.height() == null ? null : BigDecimal.valueOf(stack.height()),
			stack.horizontalLength() == null ? null : BigDecimal.valueOf(stack.horizontalLength()),
			stack.verticalLength() == null ? null : BigDecimal.valueOf(stack.verticalLength()),
			stack.orientation(),
			stack.standardOxygen() == null ? null : BigDecimal.valueOf(stack.standardOxygen())
		);
	}
}
