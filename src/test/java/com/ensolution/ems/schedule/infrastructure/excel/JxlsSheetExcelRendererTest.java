package com.ensolution.ems.schedule.infrastructure.excel;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.command.export.EquipmentExportView;
import com.ensolution.ems.schedule.application.command.export.PitotCoefficientExportView;
import com.ensolution.ems.schedule.application.command.export.PointExportView;
import com.ensolution.ems.schedule.application.command.export.ScheduleExportView;
import com.ensolution.ems.schedule.application.command.export.SheetExportView;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** jxls 렌더러가 export 뷰를 템플릿 표현식·반복(jx:each)에 실제로 바인딩하는지 검증하는 런타임 테스트. */
class JxlsSheetExcelRendererTest {

	private final JxlsSheetExcelRenderer renderer = new JxlsSheetExcelRenderer();

	private ScheduleExportView view() {
		PointExportView p1 = PointExportView.builder()
			.index(1).gasTemperature(new BigDecimal("100")).dynamicPressure(new BigDecimal("5"))
			.staticPressure(new BigDecimal("-2")).gasVelocity(new BigDecimal("12.3"))
			.kFactor(new BigDecimal("0.5")).isokineticRatio(new BigDecimal("98.7")).build();
		PointExportView p2 = PointExportView.builder()
			.index(2).gasTemperature(new BigDecimal("101")).dynamicPressure(new BigDecimal("6"))
			.staticPressure(new BigDecimal("-3")).gasVelocity(new BigDecimal("13.1"))
			.kFactor(new BigDecimal("0.6")).isokineticRatio(new BigDecimal("99.1")).build();
		SheetExportView sheet = SheetExportView.builder()
			.category("먼지")
			.samplingPointCount(2)
			.atmosphericPressure(new BigDecimal("760.0"))
			.oxygenCorrectionFactor(new BigDecimal("1.54545"))
			.area(new BigDecimal("0.785"))
			.pitotCoefficient(new BigDecimal("0.84"))
			.gasVelocity(new BigDecimal("12.3"))
			.quantity(new BigDecimal("1000.0"))
			.standardQuantity(new BigDecimal("850.0"))
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
		SheetExportView dust = SheetExportView.builder().category("먼지").samplingPointCount(1).build();
		SheetExportView gas = SheetExportView.builder().category("가스상").samplingPointCount(1).build();
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
			a1.setCellValue("${plan.referenceNumber}");            // 원장 데이터
			s.createRow(1).createCell(0).setCellValue("${sheet.category}"); // 단일 시트
			addComment(wb, s, a1, "jx:area(lastCell=\"A2\")");
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
				}
			}
		}

		assertThat(entryNames).containsExactly("1_먼지.xlsx", "2_가스상.xlsx");
		assertThat(categories).containsExactly("먼지", "가스상");
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
