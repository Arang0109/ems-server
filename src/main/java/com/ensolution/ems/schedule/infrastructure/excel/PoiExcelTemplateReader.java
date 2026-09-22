package com.ensolution.ems.schedule.infrastructure.excel;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.schedule.application.command.export.TemplateExpressionRef;
import com.ensolution.ems.schedule.application.command.export.TemplateExpressionRef.Source;
import com.ensolution.ems.schedule.application.port.out.ExcelTemplateReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jxls.builder.JxlsTemplateFillerBuilder;
import org.jxls.builder.xls.XlsCommentAreaBuilder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * POI로 템플릿을 열어 jxls 표현식을 뽑는다. <b>렌더러와 같은 규칙</b>으로 읽어야 검사 결과가 실제 렌더링과 일치하므로,
 * 셀 텍스트의 표현식 패턴은 jxls의 {@code ExpressionEvaluatorContext}가 쓰는 정규식({@code \Q${\E(.+?)\Q}\E})을,
 * 메모 명령의 접두·속성 문법은 {@link XlsCommentAreaBuilder}의 것을 그대로 따른다.
 * <p>
 * 표현식의 변수 경로는 JEXL 파서({@code JexlScript#getVariables})로 뽑는다 — 정규식으로 흉내내면 대괄호 인덱스·
 * 문자열 키·연산자가 섞인 표현식에서 어긋난다. 이 클래스는 <b>읽기만</b> 하며 무엇이 알려진 이름인지는 모른다.
 */
@Slf4j
@Component
public class PoiExcelTemplateReader implements ExcelTemplateReader {

	private static final Pattern EXPRESSION_PATTERN = Pattern.compile(
		Pattern.quote(JxlsTemplateFillerBuilder.DEFAULT_EXPRESSION_BEGIN) + "(.+?)"
			+ Pattern.quote(JxlsTemplateFillerBuilder.DEFAULT_EXPRESSION_END));

	/** {@code XlsCommentAreaBuilder.ATTR_REGEX}와 같다 — 따옴표 종류(ASCII·유니코드 인용부호)까지 맞춘다. */
	private static final Pattern ATTR_PATTERN = Pattern.compile(
		"\\s*\\w+\\s*=\\s*([\"|'\u201C\u201D\u201E\u201F\u2033\u2036\u2018\u2019\u201A\u201B\u2032\u2035])(?:(?!\\1).)*\\1");

	/** 명령별로 평가되는 표현식을 담는 속성. 그 외 명령({@code area}·{@code params} 등)은 평가할 표현식이 없다. */
	private static final Map<String, String> EXPRESSION_ATTRIBUTE = Map.of(
		"each", "items",
		"if", "condition");

	private final JexlEngine jexl = new JexlBuilder().silent(true).strict(false).create();

	@Override
	public List<TemplateExpressionRef> readExpressions(byte[] template) {
		try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(template))) {
			List<TemplateExpressionRef> refs = new ArrayList<>();
			for (Sheet sheet : wb) {
				readSheet(sheet, refs);
			}
			return refs;
		} catch (Exception e) {
			log.warn("템플릿을 열 수 없습니다", e);
			throw new CustomException(ErrorCode.SCHEDULE_EXPORT_FAILED);
		}
	}

	/** 행·열 순으로 훑는다 — 바깥 반복의 메모가 안쪽 반복보다 먼저 나와야 검사기가 반복 변수를 순서대로 묶는다. */
	private void readSheet(Sheet sheet, List<TemplateExpressionRef> refs) {
		Map<CellAddress, ? extends Comment> comments = sheet.getCellComments();
		for (Row row : sheet) {
			for (Cell cell : row) {
				CellAddress address = cell.getAddress();
				Comment comment = comments.get(address);
				if (comment != null) {
					readComment(sheet.getSheetName(), address, comment.getString().getString(), refs);
				}
				if (cell.getCellType() == CellType.STRING) {
					readCellText(sheet.getSheetName(), address, cell.getStringCellValue(), refs);
				}
			}
		}
	}

	private void readCellText(String sheetName, CellAddress address, String text, List<TemplateExpressionRef> refs) {
		Matcher matcher = EXPRESSION_PATTERN.matcher(text);
		while (matcher.find()) {
			String expression = matcher.group(1);
			refs.add(ref(sheetName, address, Source.CELL, null, Map.of(), expression));
		}
	}

	/** 메모 한 줄이 명령 하나다. {@code jx:params}는 셀 속성이라 명령이 아니다({@link XlsCommentAreaBuilder#isCommandString}). */
	private void readComment(String sheetName, CellAddress address, String text, List<TemplateExpressionRef> refs) {
		for (String line : text.split("\\r?\\n")) {
			String trimmed = line.strip();
			if (!XlsCommentAreaBuilder.isCommandString(trimmed)) continue;

			int nameEnd = trimmed.indexOf('(', XlsCommentAreaBuilder.COMMAND_PREFIX.length());
			if (nameEnd < 0) continue;
			String command = trimmed.substring(XlsCommentAreaBuilder.COMMAND_PREFIX.length(), nameEnd).strip();
			Map<String, String> attributes = parseAttributes(trimmed.substring(nameEnd + 1));

			String expressionAttribute = EXPRESSION_ATTRIBUTE.get(command);
			String expression = expressionAttribute == null ? null : attributes.get(expressionAttribute);
			refs.add(ref(sheetName, address, Source.COMMENT, command, attributes, expression));
		}
	}

	private static Map<String, String> parseAttributes(String attrString) {
		Map<String, String> attributes = new LinkedHashMap<>();
		Matcher matcher = ATTR_PATTERN.matcher(attrString);
		while (matcher.find()) {
			String attr = matcher.group();
			int eq = attr.indexOf('=');
			String name = attr.substring(0, eq).strip();
			String quoted = attr.substring(eq + 1).strip();
			attributes.put(name, quoted.substring(1, quoted.length() - 1));
		}
		return attributes;
	}

	private TemplateExpressionRef ref(String sheetName, CellAddress address, Source source, String command,
	                                  Map<String, String> attributes, String expression) {
		if (expression == null) {
			return new TemplateExpressionRef(sheetName, address.formatAsString(), source, command, attributes,
				null, List.of(), true);
		}
		try {
			Set<List<String>> paths = jexl.createScript(expression).getVariables();
			return new TemplateExpressionRef(sheetName, address.formatAsString(), source, command, attributes,
				expression, paths.stream().map(List::copyOf).toList(), true);
		} catch (JexlException e) {
			return new TemplateExpressionRef(sheetName, address.formatAsString(), source, command, attributes,
				expression, List.of(), false);
		}
	}
}
