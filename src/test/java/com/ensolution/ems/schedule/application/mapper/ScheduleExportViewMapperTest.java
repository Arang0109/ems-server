package com.ensolution.ems.schedule.application.mapper;

import com.ensolution.ems.equipment.domain.EquipType;
import com.ensolution.ems.equipment.domain.PitotTubeType;
import com.ensolution.ems.equipment.domain.spec.EquipmentSpec;
import com.ensolution.ems.equipment.domain.spec.GasSamplerSpec;
import com.ensolution.ems.equipment.domain.spec.NozzleSpec;
import com.ensolution.ems.equipment.domain.spec.ParticleSamplerSpec;
import com.ensolution.ems.equipment.domain.spec.PitotTubeSpec;
import com.ensolution.ems.schedule.application.command.export.EquipmentExportView;
import com.ensolution.ems.schedule.application.command.export.FacilityExportView;
import com.ensolution.ems.schedule.application.command.export.PitotCoefficientExportView;
import com.ensolution.ems.schedule.application.command.export.PreventionExportView;
import com.ensolution.ems.schedule.application.command.export.ScheduleExportView;
import com.ensolution.ems.schedule.application.command.export.TargetSubstanceExportView;
import com.ensolution.ems.schedule.domain.snapshot.ClientSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.EquipmentSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.FacilitySnapshot;
import com.ensolution.ems.schedule.domain.snapshot.PreventionSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.ScheduleSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.StackSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.TargetSubstanceSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.TeamSnapshot;
import com.ensolution.ems.schedule.domain.snapshot.WorkplaceSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** 스냅샷의 장비 정보가 엑셀 뷰의 슬롯·목록·유형별 사양으로 평탄화되는지 검증한다. */
class ScheduleExportViewMapperTest {

	private final ScheduleExportViewMapper mapper = new ScheduleExportViewMapper();

	private EquipmentSnapshot equipment(String id, EquipType type, String managementNumber) {
		return equipment(id, type, managementNumber, null);
	}

	private EquipmentSnapshot equipment(String id, EquipType type, String managementNumber, EquipmentSpec spec) {
		return new EquipmentSnapshot(
			id, type, managementNumber, "SN-" + id, "모델-" + id, "장비-" + id, "별칭-" + id,
			"제조사-" + id, 12, LocalDate.of(2025, 3, 1), spec);
	}

	private TeamSnapshot team(String particleSamplerId, String gasSamplerId, String pitotTubeId, String nozzleId) {
		return new TeamSnapshot(1L, "1팀", 10L, "홍길동", 20L, "김철수",
			particleSamplerId, gasSamplerId, pitotTubeId, nozzleId);
	}

	private ScheduleSnapshot snapshot(TeamSnapshot team, List<EquipmentSnapshot> equipments) {
		return new ScheduleSnapshot("1", 1L, 1L, null, null, team, null, null, equipments, null, null);
	}

	/** 배출·방지시설이 달린 측정시설 트리(의뢰기관 → 사업장 → 측정시설)를 품은 스냅샷. */
	private ScheduleSnapshot snapshot(List<FacilitySnapshot> facilities, List<PreventionSnapshot> preventions) {
		StackSnapshot stack = new StackSnapshot(1L, null, "1번 배출구", null, null, null, null,
			null, null, null, null, null, null, facilities, preventions);
		WorkplaceSnapshot workplace = new WorkplaceSnapshot(1L, "사업장", null, null, null, null, null, stack);
		ClientSnapshot client = new ClientSnapshot(1L, "의뢰기관", null, null, null, null, null,
			null, null, null, null, workplace);
		return new ScheduleSnapshot("1", 1L, 1L, null, null, null, null, client, null, null, null);
	}

	@Test
	void 팀_슬롯_id로_장비가_매칭된다() {
		List<EquipmentSnapshot> equipments = List.of(
			equipment("E1", EquipType.PARTICLE_SAMPLER, "PS-001"),
			equipment("E2", EquipType.GAS_SAMPLER, "GS-001"),
			equipment("E3", EquipType.PITOT_TUBE, "PT-001"),
			equipment("E4", EquipType.NOZZLE, "NZ-001"));

		ScheduleExportView view = mapper.toExportView(snapshot(team("E1", "E2", "E3", "E4"), equipments));

		assertThat(view.getParticleSampler().getManagementNumber()).isEqualTo("PS-001");
		assertThat(view.getParticleSampler().getTypeLabel()).isEqualTo("입자상 채취기");
		assertThat(view.getParticleSampler().getType()).isEqualTo("PARTICLE_SAMPLER");
		assertThat(view.getGasSampler().getManagementNumber()).isEqualTo("GS-001");
		assertThat(view.getPitotTube().getModelName()).isEqualTo("모델-E3");
		assertThat(view.getNozzle().getLastCalibrationDate()).isEqualTo(LocalDate.of(2025, 3, 1));
		assertThat(view.getEquipments()).hasSize(4);
	}

	@Test
	void 슬롯_id가_없으면_장비_유형으로_대체_매칭된다() {
		List<EquipmentSnapshot> equipments = List.of(
			equipment("E1", EquipType.PARTICLE_SAMPLER, "PS-001"),
			equipment("E3", EquipType.PITOT_TUBE, "PT-001"));

		ScheduleExportView view = mapper.toExportView(snapshot(team(null, null, null, null), equipments));

		assertThat(view.getParticleSampler().getManagementNumber()).isEqualTo("PS-001");
		assertThat(view.getPitotTube().getManagementNumber()).isEqualTo("PT-001");
		assertThat(view.getGasSampler()).isNull();   // 해당 유형 장비 없음
		assertThat(view.getNozzle()).isNull();
	}

	@Test
	void 팀이_없어도_장비_유형으로_매칭된다() {
		List<EquipmentSnapshot> equipments = List.of(equipment("E4", EquipType.NOZZLE, "NZ-001"));

		ScheduleExportView view = mapper.toExportView(snapshot(null, equipments));

		assertThat(view.getNozzle().getManagementNumber()).isEqualTo("NZ-001");
		assertThat(view.getEquipments()).hasSize(1);
	}

	@Test
	void 장비가_없으면_슬롯은_null이고_목록은_비어있다() {
		ScheduleExportView view = mapper.toExportView(snapshot(team("E1", "E2", "E3", "E4"), null));

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

		ScheduleExportView view = mapper.toExportView(snapshot(team("E1", null, null, null), List.of(sampler)));

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

		ScheduleExportView view = mapper.toExportView(snapshot(team(null, "E2", null, null), List.of(gasSampler)));

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

		ScheduleExportView view = mapper.toExportView(snapshot(team(null, null, "E3", null), List.of(pitotTube)));

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

		ScheduleExportView view = mapper.toExportView(snapshot(team(null, null, null, "E4"), List.of(nozzle)));

		assertThat(view.getNozzle().getNozzleDiameters())
			.containsExactly(new BigDecimal("6.0"), new BigDecimal("8.0"));
	}

	@Test
	void 사양이_없으면_사양_필드는_null이고_목록은_비어있다() {
		EquipmentSnapshot pitotTube = equipment("E3", EquipType.PITOT_TUBE, "PT-001");   // spec == null

		ScheduleExportView view = mapper.toExportView(snapshot(team(null, null, "E3", null), List.of(pitotTube)));

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
			new FacilitySnapshot(1L, "보일러", "1,000", "500", "LNG"),
			new FacilitySnapshot(2L, "건조기", "300", "150", "경유"));

		ScheduleExportView view = mapper.toExportView(snapshot(facilities, null));

		assertThat(view.getFacilities())
			.extracting(FacilityExportView::getName, FacilityExportView::getFuelUsage,
				FacilityExportView::getFuelInput, FacilityExportView::getFuelType)
			.containsExactly(
				tuple("보일러", "1,000", "500", "LNG"),
				tuple("건조기", "300", "150", "경유"));
	}

	@Test
	void 방지시설에_측정대상물질이_중첩되고_소속명이_채워진다() {
		List<PreventionSnapshot> preventions = List.of(
			new PreventionSnapshot(1L, "흡착탑", 50.0, List.of(
				new TargetSubstanceSnapshot(10L, "THC", 90.0))));

		ScheduleExportView view = mapper.toExportView(snapshot(null, preventions));

		assertThat(view.getPreventions())
			.extracting(PreventionExportView::getName, PreventionExportView::getCapacity)
			.containsExactly(tuple("흡착탑", 50.0));
		assertThat(view.getPreventions().getFirst().getTargetSubstances())
			.extracting(TargetSubstanceExportView::getPreventionName, TargetSubstanceExportView::getName,
				TargetSubstanceExportView::getRemovalEfficiency)
			.containsExactly(tuple("흡착탑", "THC", 90.0));
	}

	@Test
	void 최상위_측정대상물질_목록이_방지시설_순서대로_펼쳐진다() {
		List<PreventionSnapshot> preventions = List.of(
			new PreventionSnapshot(1L, "흡착탑", 50.0, List.of(
				new TargetSubstanceSnapshot(10L, "THC", 90.0),
				new TargetSubstanceSnapshot(11L, "먼지", 95.0))),
			new PreventionSnapshot(2L, "여과집진기", 30.0, List.of(
				new TargetSubstanceSnapshot(12L, "황산화물", 80.0))));

		ScheduleExportView view = mapper.toExportView(snapshot(null, preventions));

		assertThat(view.getTargetSubstances())
			.extracting(TargetSubstanceExportView::getPreventionName, TargetSubstanceExportView::getName)
			.containsExactly(
				tuple("흡착탑", "THC"),
				tuple("흡착탑", "먼지"),
				tuple("여과집진기", "황산화물"));
	}

	@Test
	void 방지시설에_측정대상물질이_없어도_목록은_비어있다() {
		List<PreventionSnapshot> preventions = List.of(new PreventionSnapshot(1L, "흡착탑", 50.0, null));

		ScheduleExportView view = mapper.toExportView(snapshot(null, preventions));

		// jx:each가 깨지지 않도록 null이 아닌 빈 리스트
		assertThat(view.getPreventions().getFirst().getTargetSubstances()).isEmpty();
		assertThat(view.getTargetSubstances()).isEmpty();
	}

	@Test
	void 측정시설_트리가_없으면_시설_목록은_모두_비어있다() {
		// client == null 이므로 stack까지 도달하지 못한다
		ScheduleExportView view = mapper.toExportView(snapshot(team(null, null, null, null), List.of()));

		assertThat(view.getFacilities()).isEmpty();
		assertThat(view.getPreventions()).isEmpty();
		assertThat(view.getTargetSubstances()).isEmpty();
	}
}
