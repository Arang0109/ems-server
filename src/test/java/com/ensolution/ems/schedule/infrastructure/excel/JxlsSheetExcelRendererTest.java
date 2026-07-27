package com.ensolution.ems.schedule.infrastructure.excel;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.command.export.EquipmentExportView;
import com.ensolution.ems.schedule.application.command.export.FlowExportView;
import com.ensolution.ems.schedule.application.command.export.MoistureExportView;
import com.ensolution.ems.schedule.application.command.export.PitotCoefficientExportView;
import com.ensolution.ems.schedule.application.command.export.PointExportView;
import com.ensolution.ems.schedule.application.command.export.ScheduleExportView;
import com.ensolution.ems.schedule.application.command.export.SheetExportView;
import com.ensolution.ems.schedule.application.command.export.WeatherExportView;
import com.ensolution.ems.schedule.application.mapper.SheetExportViewMapper;
import com.ensolution.ems.schedule.domain.sheet.MeasurementSheet;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** jxls 렌더러가 export 뷰를 템플릿 표현식·반복(jx:each)에 실제로 바인딩하는지 검증하는 런타임 테스트. */
class JxlsSheetExcelRendererTest {

	private final JxlsSheetExcelRenderer renderer = new JxlsSheetExcelRenderer();

	private ScheduleExportView view() {
		PointExportView p1 = PointExportView.builder()
			.index(1).temperature(new BigDecimal("100")).dynamicPressure(new BigDecimal("5"))
			.staticPressure(new BigDecimal("-2")).velocity(new BigDecimal("12.3"))
			.avgTemperature(new BigDecimal("21"))
			.kFactor(new BigDecimal("0.5")).isokineticRatio(new BigDecimal("98.7")).build();
		PointExportView p2 = PointExportView.builder()
			.index(2).temperature(new BigDecimal("101")).dynamicPressure(new BigDecimal("6"))
			.staticPressure(new BigDecimal("-3")).velocity(new BigDecimal("13.1"))
			.avgTemperature(new BigDecimal("22"))
			.kFactor(new BigDecimal("0.6")).isokineticRatio(new BigDecimal("99.1")).build();
		SheetExportView sheet = SheetExportView.builder()
			.category("먼지")
			.pointCount(2)
			.weather(WeatherExportView.builder().pressureMmHg(new BigDecimal("760.0")).build())
			.moisture(MoistureExportView.builder().ratio(new BigDecimal("11.8")).build())
			.flow(FlowExportView.builder()
				.area(new BigDecimal("0.785"))
				.pitotCoefficient(new BigDecimal("0.84"))
				.velocity(new BigDecimal("12.3"))
				.quantity(new BigDecimal("1000.0"))
				.standardQuantity(new BigDecimal("850.0"))
				.build())
			.points(List.of(p1, p2))
			.build();
		return ScheduleExportView.builder()
			.referenceNumber("REF-123")
			.analyst("홍길동")
			.measurementField("대기")
			.clientName("의뢰기관A")
			.workplaceName("사업장B")
			.stackName("1호 굴뚝")
			.stackShape("원형")
			.sheets(List.of(sheet))
			.build();
	}

	// 시트 2개짜리 뷰 (채취기록부: 시트별 파일 분리 검증용)
	private ScheduleExportView twoSheetView() {
		SheetExportView dust = SheetExportView.builder().category("먼지").pointCount(1)
			.flow(FlowExportView.builder().velocity(new BigDecimal("12.3")).build()).build();
		SheetExportView gas = SheetExportView.builder().category("가스상").pointCount(1)
			.flow(FlowExportView.builder().velocity(new BigDecimal("9.9")).build()).build();
		return ScheduleExportView.builder()
			.referenceNumber("REF-123")
			.clientName("의뢰기관A")
			.sheets(List.of(dust, gas))
			.build();
	}

	// 단일 시트(sheet 변수) + 원장 데이터(plan) 를 바인딩하는 채취기록부 템플릿
	private byte[] singleSheetTemplate() throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			XSSFSheet s = wb.createSheet("Record");
			XSSFCell a1 = s.createRow(0).createCell(0);
			a1.setCellValue("${plan.referenceNumber}");                    // 원장 데이터
			s.createRow(1).createCell(0).setCellValue("${sheet.category}"); // 단일 시트
			s.createRow(2).createCell(0).setCellValue("${sheet.flow.velocity}"); // 하위 그룹 경로
			addComment(wb, s, a1, "jx:area(lastCell=\"A3\")");
			wb.write(out);
			return out.toByteArray();
		}
	}

	// 채취기록부에서 측정 영역별 하위 뷰를 최상위 변수로 참조하는 템플릿
	private byte[] flattenedGroupTemplate() throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			XSSFSheet s = wb.createSheet("Record");
			XSSFCell a1 = s.createRow(0).createCell(0);
			a1.setCellValue("${plan.referenceNumber}/${sheet.category}"
				+ "/${weather.pressureMmHg}/${moisture.ratio}/${flow.velocity}");
			XSSFCell a2 = s.createRow(1).createCell(0);
			a2.setCellValue("${p.index}=${p.temperature}");
			addComment(wb, s, a1, "jx:area(lastCell=\"A2\")");
			addComment(wb, s, a2, "jx:each(items=\"points\" var=\"p\" lastCell=\"A2\")");
			wb.write(out);
			return out.toByteArray();
		}
	}

	// A1:A2 를 jx:area 로 지정하고 단순 표현식을 넣은 템플릿
	private byte[] simpleTemplate() throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			XSSFSheet s = wb.createSheet("Report");
			XSSFCell a1 = s.createRow(0).createCell(0);
			a1.setCellValue("${plan.referenceNumber}");
			s.createRow(1).createCell(0).setCellValue("${plan.clientName}");
			addComment(wb, s, a1, "jx:area(lastCell=\"A2\")");
			wb.write(out);
			return out.toByteArray();
		}
	}

	// 시트의 측정 영역별 하위 그룹(weather/moisture/flow)과 측정점 반복을 중첩 경로로 참조하는 템플릿
	private byte[] nestedGroupTemplate() throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			XSSFSheet s = wb.createSheet("Report");
			XSSFCell a1 = s.createRow(0).createCell(0);
			a1.setCellValue("${plan.sheets[0].weather.pressureMmHg}/${plan.sheets[0].moisture.ratio}"
				+ "/${plan.sheets[0].flow.standardQuantity}");
			XSSFCell a2 = s.createRow(1).createCell(0);
			a2.setCellValue("${p.index}=${p.avgTemperature}");
			addComment(wb, s, a1, "jx:area(lastCell=\"A2\")");
			addComment(wb, s, a2, "jx:each(items=\"plan.sheets[0].points\" var=\"p\" lastCell=\"A2\")");
			wb.write(out);
			return out.toByteArray();
		}
	}

	// 값이 하나도 없는 시트에서도 하위 그룹 경로와 빈 목록 반복이 견디는지 확인하는 템플릿
	private byte[] emptySheetTemplate() throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			XSSFSheet s = wb.createSheet("Report");
			XSSFCell a1 = s.createRow(0).createCell(0);
			a1.setCellValue("[${plan.sheets[0].weather.temperature}][${plan.sheets[0].moisture.ratio}]");
			XSSFCell a2 = s.createRow(1).createCell(0);
			a2.setCellValue("${o}");
			addComment(wb, s, a1, "jx:area(lastCell=\"A2\")");
			addComment(wb, s, a2, "jx:each(items=\"plan.sheets[0].gas.o2\" var=\"o\" lastCell=\"A2\")");
			wb.write(out);
			return out.toByteArray();
		}
	}

	// A1(area) + A2 에 jx:each 로 측정점 반복
	private byte[] loopTemplate() throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			XSSFSheet s = wb.createSheet("Report");
			XSSFCell a1 = s.createRow(0).createCell(0);
			a1.setCellValue("${plan.sheets[0].category}");
			XSSFCell a2 = s.createRow(1).createCell(0);
			a2.setCellValue("${p.index}=${p.kFactor}");
			addComment(wb, s, a1, "jx:area(lastCell=\"A2\")");
			addComment(wb, s, a2, "jx:each(items=\"plan.sheets[0].points\" var=\"p\" lastCell=\"A2\")");
			wb.write(out);
			return out.toByteArray();
		}
	}

	// 장비 슬롯 단건 참조 + 장비 목록 반복을 바인딩하는 뷰/템플릿
	private ScheduleExportView equipmentView() {
		EquipmentExportView sampler = EquipmentExportView.builder()
			.type("PARTICLE_SAMPLER").typeLabel("입자상 채취기")
			.managementNumber("PS-001").modelName("APEX-500").manufacturer("엔솔루션").build();
		EquipmentExportView nozzle = EquipmentExportView.builder()
			.type("NOZZLE").typeLabel("노즐")
			.managementNumber("NZ-001").modelName("NZ-Std").manufacturer("엔솔루션").build();
		return ScheduleExportView.builder()
			.referenceNumber("REF-123")
			.particleSampler(sampler)
			.nozzle(nozzle)
			.equipments(List.of(sampler, nozzle))
			.sheets(List.of(SheetExportView.builder().category("먼지").build()))
			.build();
	}

	private byte[] equipmentTemplate() throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			XSSFSheet s = wb.createSheet("Report");
			XSSFCell a1 = s.createRow(0).createCell(0);
			a1.setCellValue("${plan.particleSampler.managementNumber}/${plan.nozzle.modelName}");
			XSSFCell a2 = s.createRow(1).createCell(0);
			// 반복 변수명으로 eq/ne/lt 등 JEXL 예약 연산자는 쓸 수 없다(파싱 오류)
			a2.setCellValue("${equipment.typeLabel}=${equipment.managementNumber}");
			addComment(wb, s, a1, "jx:area(lastCell=\"A2\")");
			addComment(wb, s, a2, "jx:each(items=\"plan.equipments\" var=\"equipment\" lastCell=\"A2\")");
			wb.write(out);
			return out.toByteArray();
		}
	}

	// 유형별 사양(평탄화 필드 + 사양 목록)을 채운 장비 뷰
	private ScheduleExportView equipmentSpecView() {
		EquipmentExportView sampler = EquipmentExportView.builder()
			.type("PARTICLE_SAMPLER").typeLabel("입자상 채취기").managementNumber("PS-001")
			.totalVolume(new BigDecimal("2.5")).orificeDeltaH(new BigDecimal("46.3")).yd(new BigDecimal("1.002"))
			.coefficients(List.of()).nozzleDiameters(List.of()).build();
		EquipmentExportView pitotTube = EquipmentExportView.builder()
			.type("PITOT_TUBE").typeLabel("피토관").managementNumber("PT-001")
			.pitotTubeType("FINE_DUST").pitotTubeTypeLabel("미세먼지")
			.coefficients(List.of(
				PitotCoefficientExportView.builder()
					.velocity(new BigDecimal("5")).coefficient(new BigDecimal("0.84")).build(),
				PitotCoefficientExportView.builder()
					.velocity(new BigDecimal("10")).coefficient(new BigDecimal("0.85")).build()))
			.nozzleDiameters(List.of()).build();
		EquipmentExportView nozzle = EquipmentExportView.builder()
			.type("NOZZLE").typeLabel("노즐").managementNumber("NZ-001")
			.coefficients(List.of())
			.nozzleDiameters(List.of(new BigDecimal("6.0"), new BigDecimal("8.0"))).build();
		return ScheduleExportView.builder()
			.referenceNumber("REF-123")
			.particleSampler(sampler)
			.pitotTube(pitotTube)
			.nozzle(nozzle)
			.equipments(List.of(sampler, pitotTube, nozzle))
			.sheets(List.of(SheetExportView.builder().category("먼지").build()))
			.build();
	}

	// A1(area) 에 사양 단건 값, A2 에 피토관 계수표, A3 에 노즐경 목록을 반복
	private byte[] equipmentSpecTemplate() throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			XSSFSheet s = wb.createSheet("Report");
			XSSFCell a1 = s.createRow(0).createCell(0);
			a1.setCellValue("${plan.particleSampler.orificeDeltaH}/${plan.pitotTube.pitotTubeTypeLabel}");
			XSSFCell a2 = s.createRow(1).createCell(0);
			// 반복 변수명으로 eq/ne/lt 등 JEXL 예약 연산자는 쓸 수 없다(파싱 오류)
			a2.setCellValue("${pc.velocity}:${pc.coefficient}");
			XSSFCell a3 = s.createRow(2).createCell(0);
			a3.setCellValue("${nd}");
			addComment(wb, s, a1, "jx:area(lastCell=\"A3\")");
			addComment(wb, s, a2, "jx:each(items=\"plan.pitotTube.coefficients\" var=\"pc\" lastCell=\"A2\")");
			addComment(wb, s, a3, "jx:each(items=\"plan.nozzle.nozzleDiameters\" var=\"nd\" lastCell=\"A3\")");
			wb.write(out);
			return out.toByteArray();
		}
	}

	private void addComment(XSSFWorkbook wb, XSSFSheet s, XSSFCell cell, String text) {
		XSSFCreationHelper help = wb.getCreationHelper();
		XSSFDrawing drawing = s.createDrawingPatriarch();
		XSSFClientAnchor anchor = help.createClientAnchor();
		anchor.setCol1(cell.getColumnIndex());
		anchor.setCol2(cell.getColumnIndex() + 3);
		anchor.setRow1(cell.getRowIndex());
		anchor.setRow2(cell.getRowIndex() + 3);
		XSSFComment comment = drawing.createCellComment(anchor);
		comment.setString(help.createRichTextString(text));
		cell.setCellComment(comment);
	}

	@Test
	void 단순_표현식이_바인딩된다() throws Exception {
		byte[] rendered = renderer.render(simpleTemplate(), view());

		try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(rendered))) {
			Sheet s = wb.getSheetAt(0);
			assertThat(s.getRow(0).getCell(0).getStringCellValue()).isEqualTo("REF-123");
			assertThat(s.getRow(1).getCell(0).getStringCellValue()).isEqualTo("의뢰기관A");
		}
	}

	@Test
	void 시트의_하위_그룹_경로가_바인딩된다() throws Exception {
		byte[] rendered = renderer.render(nestedGroupTemplate(), view());

		try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(rendered))) {
			Sheet s = wb.getSheetAt(0);
			assertThat(s.getRow(0).getCell(0).getStringCellValue()).isEqualTo("760.0/11.8/850.0");
			// 측정점 2개가 A2, A3 로 전개되고 가스미터 평균온도가 채워진다
			assertThat(s.getRow(1).getCell(0).getStringCellValue()).isEqualTo("1=21");
			assertThat(s.getRow(2).getCell(0).getStringCellValue()).isEqualTo("2=22");
		}
	}

	@Test
	void 채취기록부는_측정_영역을_최상위_변수로도_노출한다() throws Exception {
		byte[] zip = renderer.renderSamplingRecordsZip(flattenedGroupTemplate(), view());

		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
			assertThat(zis.getNextEntry()).isNotNull();
			try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(zis.readAllBytes()))) {
				Sheet s = wb.getSheetAt(0);
				// sheet.weather.pressureMmHg 를 weather.pressureMmHg 로 짧게 쓸 수 있다
				assertThat(s.getRow(0).getCell(0).getStringCellValue()).isEqualTo("REF-123/먼지/760.0/11.8/12.3");
				// 측정점 목록도 sheet 없이 반복할 수 있다
				assertThat(s.getRow(1).getCell(0).getStringCellValue()).isEqualTo("1=100");
				assertThat(s.getRow(2).getCell(0).getStringCellValue()).isEqualTo("2=101");
			}
		}
	}

	@Test
	void 값이_없는_시트도_하위_그룹_경로에서_깨지지_않는다() throws Exception {
		// 렌더러가 withExceptionThrower()로 동작하므로, 매퍼가 하위 뷰를 항상 채운다는 계약이 깨지면 여기서 실패한다
		SheetExportView emptySheet = new SheetExportViewMapper().toSheetView(MeasurementSheet.builder().build());
		ScheduleExportView plan = ScheduleExportView.builder()
			.referenceNumber("REF-123").sheets(List.of(emptySheet)).build();

		byte[] template = emptySheetTemplate();
		assertThatCode(() -> renderer.render(template, plan)).doesNotThrowAnyException();

		try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(renderer.render(template, plan)))) {
			Sheet s = wb.getSheetAt(0);
			assertThat(s.getRow(0).getCell(0).getStringCellValue()).isEqualTo("[][]");
		}
	}

	@Test
	void 측정점_반복이_전개된다() throws Exception {
		byte[] rendered = renderer.render(loopTemplate(), view());

		try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(rendered))) {
			Sheet s = wb.getSheetAt(0);
			assertThat(s.getRow(0).getCell(0).getStringCellValue()).isEqualTo("먼지");
			// 측정점 2개가 A2, A3 로 전개됨
			assertThat(s.getRow(1).getCell(0).getStringCellValue()).isEqualTo("1=0.5");
			assertThat(s.getRow(2).getCell(0).getStringCellValue()).isEqualTo("2=0.6");
		}
	}

	@Test
	void 장비_슬롯과_장비_목록이_바인딩된다() throws Exception {
		byte[] rendered = renderer.render(equipmentTemplate(), equipmentView());

		try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(rendered))) {
			Sheet s = wb.getSheetAt(0);
			assertThat(s.getRow(0).getCell(0).getStringCellValue()).isEqualTo("PS-001/NZ-Std");
			// 장비 2개가 A2, A3 로 전개됨
			assertThat(s.getRow(1).getCell(0).getStringCellValue()).isEqualTo("입자상 채취기=PS-001");
			assertThat(s.getRow(2).getCell(0).getStringCellValue()).isEqualTo("노즐=NZ-001");
		}
	}

	@Test
	void 장비_사양과_사양_목록이_바인딩된다() throws Exception {
		byte[] rendered = renderer.render(equipmentSpecTemplate(), equipmentSpecView());

		try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(rendered))) {
			Sheet s = wb.getSheetAt(0);
			assertThat(s.getRow(0).getCell(0).getStringCellValue()).isEqualTo("46.3/미세먼지");
			// 피토관 계수 2행이 A2, A3 로 전개되고 노즐경 2행이 그 아래로 밀린다
			assertThat(s.getRow(1).getCell(0).getStringCellValue()).isEqualTo("5:0.84");
			assertThat(s.getRow(2).getCell(0).getStringCellValue()).isEqualTo("10:0.85");
			// 셀에 값 하나만 있으면 jxls가 숫자 셀로 기록한다(문자열 연결과 달리)
			assertThat(s.getRow(3).getCell(0).getNumericCellValue()).isEqualTo(6.0);
			assertThat(s.getRow(4).getCell(0).getNumericCellValue()).isEqualTo(8.0);
		}
	}

	@Test
	void 채취기록부는_시트별로_파일이_분리된_ZIP을_만든다() throws Exception {
		byte[] zip = renderer.renderSamplingRecordsZip(singleSheetTemplate(), twoSheetView());

		List<String> entryNames = new ArrayList<>();
		List<String> categories = new ArrayList<>();
		List<Double> velocities = new ArrayList<>();
		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				entryNames.add(entry.getName());
				byte[] rendered = zis.readAllBytes();
				try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(rendered))) {
					Sheet s = wb.getSheetAt(0);
					// 원장 데이터(plan)와 단일 시트(sheet)가 함께 바인딩된다
					assertThat(s.getRow(0).getCell(0).getStringCellValue()).isEqualTo("REF-123");
					categories.add(s.getRow(1).getCell(0).getStringCellValue());
					// 셀에 값 하나만 있으면 jxls가 숫자 셀로 기록한다
					velocities.add(s.getRow(2).getCell(0).getNumericCellValue());
				}
			}
		}

		assertThat(entryNames).containsExactly("1_먼지.xlsx", "2_가스상.xlsx");
		assertThat(categories).containsExactly("먼지", "가스상");
		assertThat(velocities).containsExactly(12.3, 9.9);
	}

	@Test
	void 채취기록부는_시트가_없으면_예외를_던진다() throws Exception {
		ScheduleExportView noSheets = ScheduleExportView.builder().referenceNumber("REF-123").sheets(List.of()).build();

		byte[] template = singleSheetTemplate();
		assertThatThrownBy(() -> renderer.renderSamplingRecordsZip(template, noSheets))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.SCHEDULE_EXPORT_FAILED);
	}
}
