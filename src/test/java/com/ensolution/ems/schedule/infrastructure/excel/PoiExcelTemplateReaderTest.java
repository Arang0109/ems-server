package com.ensolution.ems.schedule.infrastructure.excel;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.command.export.TemplateExpressionRef;
import com.ensolution.ems.schedule.application.command.export.TemplateExpressionRef.Source;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 템플릿 리더가 jxls와 같은 규칙으로 표현식을 읽는지 고정한다 — 셀 텍스트의 {@code ${}}(수식 셀 안의 것 포함),
 * 메모의 {@code jx:} 명령과 속성, 그리고 JEXL 파서가 돌려주는 변수 경로의 모양(인덱스·문자열 키가 따옴표 없이
 * 세그먼트로 나오는지). 검사기는 이 모양에 의존한다.
 */
class PoiExcelTemplateReaderTest {

	private final PoiExcelTemplateReader reader = new PoiExcelTemplateReader();

	private static void addComment(XSSFWorkbook wb, XSSFSheet s, XSSFCell cell, String text) {
		XSSFCreationHelper help = wb.getCreationHelper();
		XSSFDrawing drawing = s.getDrawingPatriarch() == null ? s.createDrawingPatriarch() : s.getDrawingPatriarch();
		XSSFClientAnchor anchor = help.createClientAnchor();
		anchor.setCol1(cell.getColumnIndex());
		anchor.setCol2(cell.getColumnIndex() + 3);
		anchor.setRow1(cell.getRowIndex());
		anchor.setRow2(cell.getRowIndex() + 3);
		XSSFComment comment = drawing.createCellComment(anchor);
		comment.setString(help.createRichTextString(text));
		cell.setCellComment(comment);
	}

	private static byte[] template() throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			XSSFSheet s = wb.createSheet("Record");
			XSSFCell a1 = s.createRow(0).createCell(0);
			a1.setCellValue("굴뚝: ${plan.stackName} / ${items[0].name} / ${custom['siteCode']}");
			addComment(wb, s, a1, "jx:area(lastCell=\"C5\")");

			XSSFCell b2 = s.createRow(1).createCell(1);
			b2.setCellValue("$[SUM(${points[0].ts})]");   // jxls 수식 셀 안의 표현식도 평가 대상이다

			XSSFCell a3 = s.createRow(2).createCell(0);
			a3.setCellValue("${p.ts}");
			addComment(wb, s, a3, "jx:each(items=\"points\" var=\"p\" varIndex=\"i\" lastCell=\"C3\")\njx:params(formulaStrategy=\"BY_COLUMN\")");

			XSSFCell a4 = s.createRow(3).createCell(0);
			a4.setCellValue("${plan.name +}");   // 문법 오류 (plan..name 은 JEXL 범위 연산자라 유효하다)

			XSSFCell a5 = s.createRow(4).createCell(0);
			a5.setCellValue("no expression here");
			addComment(wb, s, a5, "jx:if(condition=\"plan.standardOxygen > 0\" lastCell=\"A5\")");

			wb.createSheet("Notes").createRow(0).createCell(0).setCellValue("${custom.memo}");
			wb.write(out);
			return out.toByteArray();
		}
	}

	@Test
	void 셀_텍스트와_메모_명령을_시트_행_열_순으로_읽는다() throws Exception {
		List<TemplateExpressionRef> refs = reader.readExpressions(template());

		assertThat(refs).extracting(TemplateExpressionRef::sheetName, TemplateExpressionRef::cellAddress,
				TemplateExpressionRef::source, TemplateExpressionRef::command, TemplateExpressionRef::expression)
			.containsExactly(
				org.assertj.core.groups.Tuple.tuple("Record", "A1", Source.COMMENT, "area", null),
				org.assertj.core.groups.Tuple.tuple("Record", "A1", Source.CELL, null, "plan.stackName"),
				org.assertj.core.groups.Tuple.tuple("Record", "A1", Source.CELL, null, "items[0].name"),
				org.assertj.core.groups.Tuple.tuple("Record", "A1", Source.CELL, null, "custom['siteCode']"),
				org.assertj.core.groups.Tuple.tuple("Record", "B2", Source.CELL, null, "points[0].ts"),
				org.assertj.core.groups.Tuple.tuple("Record", "A3", Source.COMMENT, "each", "points"),
				org.assertj.core.groups.Tuple.tuple("Record", "A3", Source.CELL, null, "p.ts"),
				org.assertj.core.groups.Tuple.tuple("Record", "A4", Source.CELL, null, "plan.name +"),
				org.assertj.core.groups.Tuple.tuple("Record", "A5", Source.COMMENT, "if", "plan.standardOxygen > 0"),
				org.assertj.core.groups.Tuple.tuple("Notes", "A1", Source.CELL, null, "custom.memo"));
	}

	@Test
	void 메모_명령의_속성을_읽고_params_는_명령으로_보지_않는다() throws Exception {
		List<TemplateExpressionRef> refs = reader.readExpressions(template());

		TemplateExpressionRef each = refs.stream().filter(r -> r.isCommand("each")).findFirst().orElseThrow();
		assertThat(each.attributes()).containsExactlyInAnyOrderEntriesOf(
			Map.of("items", "points", "var", "p", "varIndex", "i", "lastCell", "C3"));
		assertThat(refs).noneMatch(r -> r.isCommand("params"));
	}

	/** 검사기가 의존하는 경로 모양 — 인덱스와 문자열 키가 따옴표 없이 세그먼트로 나온다. */
	@Test
	void 변수_경로는_JEXL_파서가_세그먼트로_돌려준다() throws Exception {
		List<TemplateExpressionRef> refs = reader.readExpressions(template());

		assertThat(pathsOf(refs, "items[0].name")).containsExactly(List.of("items", "0", "name"));
		assertThat(pathsOf(refs, "custom['siteCode']")).containsExactly(List.of("custom", "siteCode"));
		assertThat(pathsOf(refs, "plan.stackName")).containsExactly(List.of("plan", "stackName"));
		assertThat(pathsOf(refs, "plan.standardOxygen > 0")).containsExactly(List.of("plan", "standardOxygen"));
		assertThat(pathsOf(refs, "custom.memo")).containsExactly(List.of("custom", "memo"));
	}

	@Test
	void 문법_오류_표현식은_parsable_false_로_표시한다() throws Exception {
		List<TemplateExpressionRef> refs = reader.readExpressions(template());

		TemplateExpressionRef broken = refs.stream().filter(r -> "plan.name +".equals(r.expression())).findFirst().orElseThrow();
		assertThat(broken.parsable()).isFalse();
		assertThat(broken.variablePaths()).isEmpty();
		assertThat(refs.stream().filter(r -> r.expression() != null && !"plan.name +".equals(r.expression())))
			.allMatch(TemplateExpressionRef::parsable);
	}

	@Test
	void xlsx_가_아니면_템플릿_처리_실패로_거부한다() {
		assertThatThrownBy(() -> reader.readExpressions("not an xlsx".getBytes()))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_EXPORT_FAILED);
	}

	private static List<List<String>> pathsOf(List<TemplateExpressionRef> refs, String expression) {
		return refs.stream().filter(r -> expression.equals(r.expression())).findFirst().orElseThrow().variablePaths();
	}
}
