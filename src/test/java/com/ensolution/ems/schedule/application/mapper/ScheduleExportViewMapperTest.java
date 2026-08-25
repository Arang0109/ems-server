package com.ensolution.ems.schedule.application.mapper;

import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.domain.InspectionItem;
import com.ensolution.ems.equipment.domain.InspectionType;
import com.ensolution.ems.equipment.domain.PitotTubeType;
import com.ensolution.ems.equipment.domain.spec.EquipmentSpec;
import com.ensolution.ems.equipment.domain.spec.GasSamplerSpec;
import com.ensolution.ems.equipment.domain.spec.NozzleSpec;
import com.ensolution.ems.equipment.domain.spec.ParticleSamplerSpec;
import com.ensolution.ems.equipment.domain.spec.PitotTubeSpec;
import com.ensolution.ems.global.common.enums.MeasurementCycle;
import com.ensolution.ems.global.common.enums.Orientation;
import com.ensolution.ems.global.common.enums.Shape;
import com.ensolution.ems.schedule.application.command.export.EquipmentExportView;
import com.ensolution.ems.schedule.application.command.export.EquipmentInspectionExportView;
import com.ensolution.ems.schedule.application.command.export.FacilityExportView;
import com.ensolution.ems.schedule.application.command.export.PitotCoefficientExportView;
import com.ensolution.ems.schedule.application.command.export.PreventionExportView;
import com.ensolution.ems.schedule.application.command.export.SamplingItemExportView;
import com.ensolution.ems.schedule.application.command.export.ScheduleExportView;
import com.ensolution.ems.schedule.domain.analysis.AnalysisRecord;
import com.ensolution.ems.schedule.domain.sheet.MeasurementCategory;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import com.ensolution.ems.schedule.domain.snapshot.BasicInfo;
import com.ensolution.ems.schedule.domain.snapshot.ClientSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.EquipmentSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.FacilitySnapshot;
import com.ensolution.ems.schedule.domain.snapshot.PreventionSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.SamplingItemSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.StackSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.TeamSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.WorkplaceSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** 스냅샷의 장비 정보가 엑셀 뷰의 슬롯·목록·유형별 사양으로 평탄화되는지 검증한다. */
class ScheduleExportViewMapperTest {

	private final ScheduleExportViewMapper mapper = new ScheduleExportViewMapper(new SheetExportViewMapper());

	/** 실험분석정보와 무관한 검증의 기본 경로 — 분석 결과가 하나도 없는 계획. */
	private ScheduleExportView toView(ScheduleSnapshot snapshot) {
		return mapper.toExportView(snapshot, Map.of());
	}

	private EquipmentSnapshot equipment(String id, EquipType type, String managementNumber) {
		return equipment(id, type, managementNumber, null);
	}

	private EquipmentSnapshot equipment(String id, EquipType type, String managementNumber, EquipmentSpec spec) {
		return equipment(id, type, managementNumber, spec, List.of(
			new InspectionItem(InspectionType.CALIBRATION, true, 12, LocalDate.of(2025, 3, 1), null, true)));
	}

	private EquipmentSnapshot equipment(
		String id, EquipType type, String managementNumber, EquipmentSpec spec, List<InspectionItem> inspections
	) {
		return new EquipmentSnapshot(
			id, type, managementNumber, "SN-" + id, "모델-" + id, "장비-" + id, "별칭-" + id,
			"제조사-" + id, inspections, spec);
	}

	private TeamSnapshot team(String particleSamplerId, String gasSamplerId, String pitotTubeId, String nozzleId) {
		return new TeamSnapshot(1L, "1팀", 10L, "홍길동", 20L, "김철수",
			particleSamplerId, gasSamplerId, pitotTubeId, nozzleId);
	}

	private ScheduleSnapshot snapshot(TeamSnapshot team, List<EquipmentSnapshot> equipments) {
		return new ScheduleSnapshot("1", 1L, 1L, null, null, team, null, null, equipments, null, null, null, null);
	}

	/** 배출·방지시설이 달린 측정시설 트리(의뢰기관 → 사업장 → 측정시설)를 품은 스냅샷. */
	private ScheduleSnapshot snapshot(List<FacilitySnapshot> facilities, List<PreventionSnapshot> preventions) {
		StackSnapshot stack = new StackSnapshot(1L, null, "1번 배출구", null, null, null, null,
			null, null, null, null, null, facilities, preventions);
		WorkplaceSnapshot workplace = new WorkplaceSnapshot(1L, "사업장", null, null, null, null, null, null, stack);
		ClientSnapshot client = new ClientSnapshot(1L, "의뢰기관", null, null, null, null, null,
			null, null, workplace);
		return new ScheduleSnapshot("1", 1L, 1L, null, null, null, null, client, null, null, null, null, null);
	}

	@Test
	void 팀_슬롯_id로_장비가_매칭된다() {
		List<EquipmentSnapshot> equipments = List.of(
			equipment("E1", EquipType.PARTICLE_SAMPLER, "PS-001"),
			equipment("E2", EquipType.GAS_SAMPLER, "GS-001"),
			equipment("E3", EquipType.PITOT_TUBE, "PT-001"),
			equipment("E4", EquipType.NOZZLE, "NZ-001"));

		ScheduleExportView view = toView(snapshot(team("E1", "E2", "E3", "E4"), equipments));

		assertThat(view.getParticleSampler().getManagementNumber()).isEqualTo("PS-001");
		assertThat(view.getParticleSampler().getTypeLabel()).isEqualTo("입자상 시료채취장비");
		assertThat(view.getParticleSampler().getType()).isEqualTo("PARTICLE_SAMPLER");
		assertThat(view.getGasSampler().getManagementNumber()).isEqualTo("GS-001");
		assertThat(view.getPitotTube().getModelName()).isEqualTo("모델-E3");
		assertThat(view.getNozzle().getLastCalibrationDate()).isEqualTo(LocalDate.of(2025, 3, 1));
		assertThat(view.getEquipments()).hasSize(4);
	}

	@Test
	void 교정_항목에서_기존_템플릿용_교정_프로퍼티가_파생된다() {
		EquipmentSnapshot sampler = equipment("E1", EquipType.PARTICLE_SAMPLER, "PS-001", null, List.of(
			new InspectionItem(InspectionType.PRECISION_INSPECTION, true, 24, LocalDate.of(2024, 9, 1), null, true),
			new InspectionItem(InspectionType.CALIBRATION, true, 12, LocalDate.of(2025, 3, 1), null, true)));

		ScheduleExportView view = toView(snapshot(team("E1", null, null, null), List.of(sampler)));

		EquipmentExportView particleSampler = view.getParticleSampler();
		assertThat(particleSampler.getCalibrationCycle()).isEqualTo(12);
		assertThat(particleSampler.getLastCalibrationDate()).isEqualTo(LocalDate.of(2025, 3, 1));
		assertThat(particleSampler.getCalibrationDueDate()).isEqualTo(LocalDate.of(2026, 3, 1));
		assertThat(particleSampler.getInspections())
			.extracting(EquipmentInspectionExportView::getTypeLabel, EquipmentInspectionExportView::getNextDueDate)
			.containsExactly(
				tuple("정도검사", LocalDate.of(2026, 9, 1)),
				tuple("교정", LocalDate.of(2026, 3, 1)));
	}

	@Test
	void 검사_항목이_없는_구_스냅샷도_빈_목록으로_매핑된다() {
		EquipmentSnapshot legacy = equipment("E1", EquipType.PARTICLE_SAMPLER, "PS-001", null, null);

		ScheduleExportView view = toView(snapshot(team("E1", null, null, null), List.of(legacy)));

		EquipmentExportView particleSampler = view.getParticleSampler();
		assertThat(particleSampler.getInspections()).isEmpty();   // jx:each가 깨지지 않도록 null이 아닌 빈 리스트
		assertThat(particleSampler.getCalibrationCycle()).isNull();
		assertThat(particleSampler.getLastCalibrationDate()).isNull();
		assertThat(particleSampler.getCalibrationDueDate()).isNull();
	}

	@Test
	void 슬롯_id가_없으면_장비_유형으로_대체_매칭된다() {
		List<EquipmentSnapshot> equipments = List.of(
			equipment("E1", EquipType.PARTICLE_SAMPLER, "PS-001"),
			equipment("E3", EquipType.PITOT_TUBE, "PT-001"));

		ScheduleExportView view = toView(snapshot(team(null, null, null, null), equipments));

		assertThat(view.getParticleSampler().getManagementNumber()).isEqualTo("PS-001");
		assertThat(view.getPitotTube().getManagementNumber()).isEqualTo("PT-001");
		assertThat(view.getGasSampler()).isNull();   // 해당 유형 장비 없음
		assertThat(view.getNozzle()).isNull();
	}

	@Test
	void 팀이_없어도_장비_유형으로_매칭된다() {
		List<EquipmentSnapshot> equipments = List.of(equipment("E4", EquipType.NOZZLE, "NZ-001"));

		ScheduleExportView view = toView(snapshot(null, equipments));

		assertThat(view.getNozzle().getManagementNumber()).isEqualTo("NZ-001");
		assertThat(view.getEquipments()).hasSize(1);
	}

	@Test
	void 장비가_없으면_슬롯은_null이고_목록은_비어있다() {
		ScheduleExportView view = toView(snapshot(team("E1", "E2", "E3", "E4"), null));

		assertThat(view.getParticleSampler()).isNull();
		assertThat(view.getGasSampler()).isNull();
		assertThat(view.getPitotTube()).isNull();
		assertThat(view.getNozzle()).isNull();
		assertThat(view.getEquipments()).isEmpty();
	}

	@Test
	void 입자상_채취기_사양이_평탄화된다() {
		EquipmentSnapshot sampler = equipment("E1", EquipType.PARTICLE_SAMPLER, "PS-001",
			new ParticleSamplerSpec(new BigDecimal("2.5"), new BigDecimal("46.3"), new BigDecimal("1.002")));

		ScheduleExportView view = toView(snapshot(team("E1", null, null, null), List.of(sampler)));

		EquipmentExportView particleSampler = view.getParticleSampler();
		assertThat(particleSampler.getTotalVolume()).isEqualByComparingTo("2.5");
		assertThat(particleSampler.getOrificeDeltaH()).isEqualByComparingTo("46.3");  // spec의 orificeDp
		assertThat(particleSampler.getYd()).isEqualByComparingTo("1.002");
		// 다른 유형의 사양 필드는 비어있다
		assertThat(particleSampler.getPitotTubeType()).isNull();
		assertThat(particleSampler.getCoefficients()).isEmpty();
		assertThat(particleSampler.getNozzleDiameters()).isEmpty();
	}

	@Test
	void 가스_채취기_사양이_평탄화된다() {
		EquipmentSnapshot gasSampler = equipment("E2", EquipType.GAS_SAMPLER, "GS-001",
			new GasSamplerSpec(new BigDecimal("1.2")));

		ScheduleExportView view = toView(snapshot(team(null, "E2", null, null), List.of(gasSampler)));

		assertThat(view.getGasSampler().getTotalVolume()).isEqualByComparingTo("1.2");
		assertThat(view.getGasSampler().getOrificeDeltaH()).isNull();
		assertThat(view.getGasSampler().getYd()).isNull();
	}

	@Test
	void 피토관_유형과_계수표가_평탄화된다() {
		EquipmentSnapshot pitotTube = equipment("E3", EquipType.PITOT_TUBE, "PT-001",
			new PitotTubeSpec(PitotTubeType.FINE_DUST, List.of(
				new PitotTubeSpec.PitotCoefficient(new BigDecimal("0.84"), new BigDecimal("5")),
				new PitotTubeSpec.PitotCoefficient(new BigDecimal("0.85"), new BigDecimal("10")))));

		ScheduleExportView view = toView(snapshot(team(null, null, "E3", null), List.of(pitotTube)));

		EquipmentExportView pitot = view.getPitotTube();
		assertThat(pitot.getPitotTubeType()).isEqualTo("FINE_DUST");
		assertThat(pitot.getPitotTubeTypeLabel()).isEqualTo("미세먼지");
		assertThat(pitot.getCoefficients())
			.extracting(PitotCoefficientExportView::getVelocity, PitotCoefficientExportView::getCoefficient)
			.containsExactly(
				tuple(new BigDecimal("5"), new BigDecimal("0.84")),
				tuple(new BigDecimal("10"), new BigDecimal("0.85")));
	}

	@Test
	void 노즐_사양의_노즐경_목록이_평탄화된다() {
		EquipmentSnapshot nozzle = equipment("E4", EquipType.NOZZLE, "NZ-001",
			new NozzleSpec(List.of(
				new NozzleSpec.NozzleDiameter(new BigDecimal("6.0")),
				new NozzleSpec.NozzleDiameter(new BigDecimal("8.0")))));

		ScheduleExportView view = toView(snapshot(team(null, null, null, "E4"), List.of(nozzle)));

		assertThat(view.getNozzle().getNozzleDiameters())
			.containsExactly(new BigDecimal("6.0"), new BigDecimal("8.0"));
	}

	@Test
	void 사양이_없으면_사양_필드는_null이고_목록은_비어있다() {
		EquipmentSnapshot pitotTube = equipment("E3", EquipType.PITOT_TUBE, "PT-001");   // spec == null

		ScheduleExportView view = toView(snapshot(team(null, null, "E3", null), List.of(pitotTube)));

		EquipmentExportView pitot = view.getPitotTube();
		assertThat(pitot.getManagementNumber()).isEqualTo("PT-001");
		assertThat(pitot.getTotalVolume()).isNull();
		assertThat(pitot.getOrificeDeltaH()).isNull();
		assertThat(pitot.getYd()).isNull();
		assertThat(pitot.getPitotTubeType()).isNull();
		assertThat(pitot.getPitotTubeTypeLabel()).isNull();
		assertThat(pitot.getCoefficients()).isEmpty();   // jx:each가 깨지지 않도록 null이 아닌 빈 리스트
		assertThat(pitot.getNozzleDiameters()).isEmpty();
	}

	@Test
	void 배출시설_목록이_평탄화된다() {
		List<FacilitySnapshot> facilities = List.of(
			new FacilitySnapshot(1L, "보일러", "1,000", "800", "0", "500", "LNG", "Nm³"),
			new FacilitySnapshot(2L, "소각로", "300", "0", "1,200", "150", "경유", "kg"));

		ScheduleExportView view = toView(snapshot(facilities, null));

		assertThat(view.getFacilities())
			.extracting(FacilityExportView::getName, FacilityExportView::getFuelUsage,
				FacilityExportView::getProductOutput, FacilityExportView::getIncinerationAmount,
				FacilityExportView::getFuelInput, FacilityExportView::getFuelType, FacilityExportView::getUnit)
			.containsExactly(
				tuple("보일러", "1,000", "800", "0", "500", "LNG", "Nm³"),
				tuple("소각로", "300", "0", "1,200", "150", "경유", "kg"));
	}

	@Test
	void 방지시설의_대상물질과_제거효율이_뷰에_매핑된다() {
		List<PreventionSnapshot> preventions = List.of(
			new PreventionSnapshot(1L, "흡착탑", 50.0, null, "THC", "90"));

		ScheduleExportView view = toView(snapshot(null, preventions));

		assertThat(view.getPreventions())
			.extracting(PreventionExportView::getName, PreventionExportView::getCapacity,
				PreventionExportView::getTargetName, PreventionExportView::getRemovalEfficiency)
			.containsExactly(tuple("흡착탑", 50.0, "THC", "90"));
	}

	@Test
	void 방지시설_목록이_스냅샷_순서대로_매핑된다() {
		List<PreventionSnapshot> preventions = List.of(
			new PreventionSnapshot(1L, "흡착탑", 50.0, null, "THC", "90"),
			new PreventionSnapshot(2L, "여과집진기", 30.0, null, "먼지", "95"));

		ScheduleExportView view = toView(snapshot(null, preventions));

		assertThat(view.getPreventions())
			.extracting(PreventionExportView::getName, PreventionExportView::getTargetName)
			.containsExactly(
				tuple("흡착탑", "THC"),
				tuple("여과집진기", "먼지"));
	}

	@Test
	void 방지시설에_대상물질_정보가_없으면_null로_유지된다() {
		List<PreventionSnapshot> preventions = List.of(new PreventionSnapshot(1L, "흡착탑", 50.0, null, null, null));

		ScheduleExportView view = toView(snapshot(null, preventions));

		PreventionExportView prevention = view.getPreventions().getFirst();
		assertThat(prevention.getName()).isEqualTo("흡착탑");
		assertThat(prevention.getTargetName()).isNull();
		assertThat(prevention.getRemovalEfficiency()).isNull();
	}

	@Test
	void 측정시설_트리가_없으면_시설_목록은_모두_비어있다() {
		// client == null 이므로 stack까지 도달하지 못한다
		ScheduleExportView view = toView(snapshot(team(null, null, null, null), List.of()));

		assertThat(view.getFacilities()).isEmpty();
		assertThat(view.getPreventions()).isEmpty();
	}

	@Test
	void 담당자_정보가_기본정보와_팀에서_모인다() {
		BasicInfo info = new BasicInfo("REF-1", "이관리", "정입회", "박분석", "최기술",
			LocalDate.of(2026, 5, 1), null, null, null, null, null, null, "자가측정용");
		ScheduleSnapshot snapshot = new ScheduleSnapshot("1", 1L, 1L, null, info,
			team(null, null, null, null), null, null, null, null, null, null, null);

		ScheduleExportView view = toView(snapshot);

		assertThat(view.getMentor()).isEqualTo("홍길동");
		assertThat(view.getMentee()).isEqualTo("김철수");
		assertThat(view.getFacilityManager()).isEqualTo("이관리");
		assertThat(view.getSamplingWitness()).isEqualTo("정입회");
		assertThat(view.getAnalyst()).isEqualTo("박분석");
		assertThat(view.getTechnicalManager()).isEqualTo("최기술");
		assertThat(view.getSchedulePurpose()).isEqualTo("자가측정용");
	}

	@Test
	void 측정시설의_SEMS번호와_형태_방향이_평탄화된다() {
		StackSnapshot stack = new StackSnapshot(1L, null, "1번 배출구", "SEMS-001", null, null, null,
			null, null, null, Shape.CIRCULAR, Orientation.VERTICAL, null, null);
		WorkplaceSnapshot workplace = new WorkplaceSnapshot(1L, "사업장", null, null, null, null, null, null, stack);
		ClientSnapshot client = new ClientSnapshot(1L, "의뢰기관", null, null, null, null, null,
			null, null, workplace);
		ScheduleSnapshot snapshot = new ScheduleSnapshot("1", 1L, 1L, null, null, null, null, client,
			null, null, null, null, null);

		ScheduleExportView view = toView(snapshot);

		assertThat(view.getSemsNumber()).isEqualTo("SEMS-001");
		assertThat(view.getStackShape()).isEqualTo("원형");
		assertThat(view.getStackOrientation()).isEqualTo("수직");
	}

	@Test
	void 측정시설_방향이_없으면_라벨도_null이다() {
		// 측정시설 트리는 있으나 shape·orientation·semsNumber가 비어있는 스냅샷
		ScheduleExportView view = toView(snapshot(List.of(), List.of()));

		assertThat(view.getStackOrientation()).isNull();
		assertThat(view.getSemsNumber()).isNull();
	}

	private SamplingItemSnapshot item(Long pollutantId, String nameKr, String allowance) {
		return new SamplingItemSnapshot(pollutantId * 10, pollutantId, "CODE-" + pollutantId, nameKr, "EN-" + pollutantId,
			null, null, null, "장비-" + pollutantId, "방법-" + pollutantId,
			MeasurementCycle.QUARTERLY, allowance == null ? null : new BigDecimal(allowance), true);
	}

	private ScheduleSnapshot itemSnapshot(List<SamplingItemSnapshot> items) {
		return new ScheduleSnapshot("1", 1L, 1L, null, null, null, null, null, null, items, null, null, null);
	}

	@Test
	void 측정항목이_스냅샷_순서대로_매핑된다() {
		// 이 순서가 곧 성적서의 항목 배치다 — 템플릿이 items[0]~items[3] 으로 칸을 지목한다
		ScheduleExportView view = toView(itemSnapshot(List.of(
			item(3L, "황산화물", "150"),
			item(1L, "먼지", "50"),
			item(2L, "질소산화물", "200"))));

		assertThat(view.getItems())
			.extracting(SamplingItemExportView::getName, SamplingItemExportView::getAllowance)
			.containsExactly(
				tuple("황산화물", new BigDecimal("150")),
				tuple("먼지", new BigDecimal("50")),
				tuple("질소산화물", new BigDecimal("200")));
	}

	@Test
	void 측정항목의_코드_주기_장비_시험방법이_평탄화된다() {
		ScheduleExportView view = toView(itemSnapshot(List.of(item(1L, "먼지", "50"))));

		SamplingItemExportView first = view.getItems().getFirst();
		assertThat(first.getName()).isEqualTo("먼지");
		assertThat(first.getNameEn()).isEqualTo("EN-1");
		assertThat(first.getCode()).isEqualTo("CODE-1");
		assertThat(first.getCycle()).isEqualTo("QUARTERLY");
		assertThat(first.getEquipment()).isEqualTo("장비-1");
		assertThat(first.getTestMethod()).isEqualTo("방법-1");
		assertThat(first.isOxygenApplicable()).isTrue();
	}

	@Test
	void 허용기준이_미지정이면_null로_유지된다() {
		ScheduleExportView view = toView(itemSnapshot(List.of(item(1L, "매연", null))));

		assertThat(view.getItems().getFirst().getAllowance()).isNull();
	}

	@Test
	void 측정항목이_없으면_빈_목록이다() {
		// jx:each가 깨지지 않도록 null이 아닌 빈 리스트
		assertThat(toView(itemSnapshot(null)).getItems()).isEmpty();
	}

	@Test
	void 측정_시트가_시트_뷰_목록으로_위임된다() {
		MeasurementSheet sheet = MeasurementSheet.builder().category(MeasurementCategory.DUST).build();
		ScheduleSnapshot snapshot = new ScheduleSnapshot("1", 1L, 1L, null, null, null, null, null,
			null, null, List.of(sheet), null, null);

		ScheduleExportView view = toView(snapshot);

		// 시트 내부 매핑 검증은 SheetExportViewMapperTest 담당
		assertThat(view.getSheets()).hasSize(1);
		assertThat(view.getSheets().getFirst().getCategory()).isEqualTo("먼지");
	}

	// ===== 실험분석정보 합류 (analysis_records × schedule_documents.items[]) =====

	private AnalysisRecord analysis(Long pollutantId, String value, String unit) {
		return AnalysisRecord.builder()
			.id("analysis-" + pollutantId)
			.tenantId(1L)
			.scheduleId(1L)
			.pollutantId(pollutantId)
			.analysisValue(value == null ? null : new BigDecimal(value))
			.unit(unit)
			.analysisMethod("분석방법-" + pollutantId)
			.analysisEquipment("분석장비-" + pollutantId)
			.build();
	}

	@Test
	void 실험실_분석값이_측정물질로_항목에_붙는다() {
		ScheduleExportView view = mapper.toExportView(
			itemSnapshot(List.of(item(1L, "먼지", "50"))),
			Map.of(1L, analysis(1L, "12.5", "mg/Sm3")));

		SamplingItemExportView first = view.getItems().getFirst();
		assertThat(first.getAnalysisValue()).isEqualByComparingTo("12.5");
		assertThat(first.getUnit()).isEqualTo("mg/Sm3");
		assertThat(first.getAnalysisMethod()).isEqualTo("분석방법-1");
		assertThat(first.getAnalysisEquipment()).isEqualTo("분석장비-1");
	}

	@Test
	void 분석_전_항목도_목록에_남고_실험실_입력값만_비어있다() {
		// 항목을 빼면 뒤 항목이 앞칸으로 밀려 성적서 칸 배치가 어긋난다
		ScheduleExportView view = mapper.toExportView(
			itemSnapshot(List.of(item(1L, "먼지", "50"), item(2L, "질소산화물", "200"))),
			Map.of(2L, analysis(2L, "30", "ppm")));

		assertThat(view.getItems())
			.extracting(SamplingItemExportView::getName, SamplingItemExportView::getUnit)
			.containsExactly(tuple("먼지", null), tuple("질소산화물", "ppm"));
		assertThat(view.getItems().getFirst().getAnalysisValue()).isNull();
		assertThat(view.getItems().getFirst().getAnalysisMethod()).isNull();
		assertThat(view.getItems().getFirst().getAnalysisEquipment()).isNull();
	}

	@Test
	void 분석값을_붙여도_항목_순서는_스냅샷을_따른다() {
		ScheduleExportView view = mapper.toExportView(
			itemSnapshot(List.of(item(3L, "황산화물", "150"), item(1L, "먼지", "50"), item(2L, "질소산화물", "200"))),
			Map.of(1L, analysis(1L, "1", "u1"), 2L, analysis(2L, "2", "u2"), 3L, analysis(3L, "3", "u3")));

		assertThat(view.getItems())
			.extracting(SamplingItemExportView::getName)
			.containsExactly("황산화물", "먼지", "질소산화물");
	}

	@Test
	void 허용기준은_분석기록이_아니라_스냅샷_값을_쓴다() {
		// 한 칸에 두 출처가 섞이면 어느 쪽이 성적서의 판정 근거였는지 구분할 수 없게 된다
		AnalysisRecord stale = analysis(1L, "12.5", "mg/Sm3").toBuilder()
			.allowance(new BigDecimal("999"))
			.build();

		ScheduleExportView view = mapper.toExportView(
			itemSnapshot(List.of(item(1L, "먼지", "50"))), Map.of(1L, stale));

		assertThat(view.getItems().getFirst().getAllowance()).isEqualByComparingTo("50");
	}

	@Test
	void 분석_색인이_null이어도_뷰_변환이_깨지지_않는다() {
		ScheduleExportView view = mapper.toExportView(itemSnapshot(List.of(item(1L, "먼지", "50"))), null);

		assertThat(view.getItems()).hasSize(1);
		assertThat(view.getItems().getFirst().getAnalysisValue()).isNull();
	}

	@Test
	void 측정물질_식별자가_없는_항목은_분석값_없이_매핑된다() {
		SamplingItemSnapshot legacy = new SamplingItemSnapshot(
			null, null, null, "구형항목", null, null, null, null, null, null, null, null, false);

		ScheduleExportView view = mapper.toExportView(
			itemSnapshot(List.of(legacy)), Map.of(1L, analysis(1L, "12.5", "mg/Sm3")));

		assertThat(view.getItems()).hasSize(1);
		assertThat(view.getItems().getFirst().getName()).isEqualTo("구형항목");
		assertThat(view.getItems().getFirst().getAnalysisValue()).isNull();
	}

	@Test
	void 성적서_탭에서_작성한_채취시간이_항목에_실린다() {
		AnalysisRecord withTimes = analysis(1L, "12.5", "mg/Sm3").toBuilder()
			.samplingStartedAt(LocalTime.of(9, 30))
			.samplingEndedAt(LocalTime.of(10, 0))
			.build();

		ScheduleExportView view = mapper.toExportView(
			itemSnapshot(List.of(item(1L, "먼지", "50"))), Map.of(1L, withTimes));

		SamplingItemExportView first = view.getItems().getFirst();
		assertThat(first.getSamplingStartedAt()).isEqualTo(LocalTime.of(9, 30));
		assertThat(first.getSamplingEndedAt()).isEqualTo(LocalTime.of(10, 0));
	}

	@Test
	void 채취시간을_아직_작성하지_않은_항목은_시각이_비어있다() {
		// 분석값만 있고 채취시간은 없는 상태 — 두 탭이 따로 작성하므로 흔한 중간 상태다
		ScheduleExportView view = mapper.toExportView(
			itemSnapshot(List.of(item(1L, "먼지", "50"))), Map.of(1L, analysis(1L, "12.5", "mg/Sm3")));

		SamplingItemExportView first = view.getItems().getFirst();
		assertThat(first.getSamplingStartedAt()).isNull();
		assertThat(first.getSamplingEndedAt()).isNull();
		assertThat(first.getAnalysisValue()).isEqualByComparingTo("12.5");
	}

	@Test
	void 자정을_넘기는_채취시간도_그대로_실린다() {
		AnalysisRecord overnight = analysis(1L, "12.5", "mg/Sm3").toBuilder()
			.samplingStartedAt(LocalTime.of(23, 0))
			.samplingEndedAt(LocalTime.of(1, 0))
			.build();

		ScheduleExportView view = mapper.toExportView(
			itemSnapshot(List.of(item(1L, "먼지", "50"))), Map.of(1L, overnight));

		assertThat(view.getItems().getFirst().getSamplingStartedAt()).isEqualTo(LocalTime.of(23, 0));
		assertThat(view.getItems().getFirst().getSamplingEndedAt()).isEqualTo(LocalTime.of(1, 0));
	}
}
