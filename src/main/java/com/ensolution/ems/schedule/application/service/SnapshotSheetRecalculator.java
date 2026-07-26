package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.equipment.domain.spec.NozzleSpec;
import com.ensolution.ems.equipment.domain.spec.ParticleSamplerSpec;
import com.ensolution.ems.equipment.domain.spec.PitotTubeSpec;
import com.ensolution.ems.schedule.application.calculation.SheetCalculator;
import com.ensolution.ems.schedule.application.command.StackData;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
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
 * 계산 입력 추출(표준산소농도·굴뚝 형상/치수·피토관 계수·노즐경)이라는 응집된 책임을 담당해
 * {@link ScheduleService}가 유스케이스 조율에 집중하도록 한다.
 */
@Component
@RequiredArgsConstructor
public class SnapshotSheetRecalculator {

	private final SheetCalculator sheetCalculator;

	/** 스냅샷의 장비 spec·측정시설 정보를 계산 입력으로 삼아 시트를 재계산한다. */
	public List<MeasurementSheet> recalculate(ScheduleSnapshot snapshot, List<MeasurementSheet> sheets) {
		if (sheets == null || sheets.isEmpty()) return sheets;

		StackData stackData = resolveStackData(snapshot);
		if (stackData == null) return sheets;   // 측정시설 스냅샷이 없으면 계산 입력이 없으므로 원본을 유지한다.

		List<PitotTubeSpec.PitotCoefficient> pitotCoefficients = resolvePitotCoefficients(snapshot);
		List<NozzleSpec.NozzleDiameter> nozzleDiameters = resolveNozzleSizes(snapshot);
		BigDecimal deltaH = resolveDeltaH(snapshot);

		return sheets.stream()
			.map(sheet -> sheetCalculator.calculate(sheet, stackData, pitotCoefficients, nozzleDiameters, deltaH))
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
