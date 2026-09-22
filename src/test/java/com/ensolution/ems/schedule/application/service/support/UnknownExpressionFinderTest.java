package com.ensolution.ems.schedule.application.service.support;

import com.ensolution.ems.schedule.application.command.export.TemplateExpressionRef;
import com.ensolution.ems.schedule.application.command.export.TemplateExpressionRef.Source;
import com.ensolution.ems.schedule.application.command.export.TemplateIssue;
import com.ensolution.ems.schedule.application.command.export.TemplateIssueType;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * 템플릿 검사기의 판정 규칙을 고정한다.
 *
 * <p>가이드가 안내하는 대표 경로는 전부 통과해야 하고(리플렉션이 Lombok getter·boolean {@code is}·목록 원소 타입·
 * JDK 하위 프로퍼티를 제대로 읽는지), 오타는 실패 지점까지의 점 경로로 잡히며, 반복 변수는 {@code items}의 원소
 * 타입에 묶이고, {@code custom} 아래 키는 정의와 대조된다. 표현식 → 경로 변환은 리더와 같은 JEXL 파서를 쓴다.
 */
class UnknownExpressionFinderTest {

	private static final JexlEngine JEXL = new JexlBuilder().silent(true).strict(false).create();
	private static final Set<String> CUSTOM_KEYS = Set.of("siteCode", "inspector");

	private final UnknownExpressionFinder finder = new UnknownExpressionFinder();

	private static TemplateExpressionRef cell(String address, String expression) {
		return ref(address, Source.CELL, null, Map.of(), expression);
	}

	private static TemplateExpressionRef area(String address) {
		return ref(address, Source.COMMENT, "area", Map.of("lastCell", "Z99"), null);
	}

	private static TemplateExpressionRef each(String address, String items, String var) {
		return ref(address, Source.COMMENT, "each", Map.of("items", items, "var", var, "lastCell", "Z99"), items);
	}

	private static TemplateExpressionRef ref(String address, Source source, String command,
	                                         Map<String, String> attributes, String expression) {
		if (expression == null) {
			return new TemplateExpressionRef("Record", address, source, command, attributes, null, List.of(), true);
		}
		try {
			List<List<String>> paths = JEXL.createScript(expression).getVariables().stream()
				.map(p -> (List<String>) new ArrayList<>(p)).toList();
			return new TemplateExpressionRef("Record", address, source, command, attributes, expression, paths, true);
		} catch (JexlException e) {
			return new TemplateExpressionRef("Record", address, source, command, attributes, expression, List.of(), false);
		}
	}

	private List<TemplateIssue> find(TemplateExpressionRef... refs) {
		List<TemplateExpressionRef> all = new ArrayList<>(List.of(area("A1")));
		all.addAll(List.of(refs));
		return finder.find(all, CUSTOM_KEYS);
	}

	@Nested
	@DisplayName("가이드의 대표 경로")
	class KnownPaths {

		@ParameterizedTest
		@ValueSource(strings = {
			"plan.referenceNumber", "plan.stackName", "plan.sampledAt.year", "plan.standardOxygen",
			"plan.facilities[0].fuelType", "plan.preventions[1].removalEfficiency",
			"plan.pitotTube.coefficients[0].velocity", "plan.nozzle.nozzleDiameters[0]",
			"plan.equipments[0].inspections[0].nextDueDate",
			"items[3].oxygenApplicable", "items[0].analysisValue", "plan.items[2].name",
			"sheet.category", "sheet.samplingPointCount", "weather.pressureHpa", "moisture.xw",
			"gas.o2[0]", "flow.avgVs", "particle.totalVm", "points[0].ts", "gaseousSamplings[0].samplingVolume",
			"custom.siteCode", "custom['inspector']", "custom.siteCode.length()", "items.size()",
			"plan.standardOxygen > 0 and moisture.xw != null"
		})
		void 계약에_있는_경로는_문제가_없다(String expression) {
			assertThat(find(cell("B2", expression))).isEmpty();
		}
	}

	@Nested
	@DisplayName("알 수 없는 이름")
	class Unknown {

		@Test
		void 최상위_변수가_아니면_UNKNOWN_ROOT() {
			assertThat(find(cell("B2", "pointz[0].ts")))
				.extracting(TemplateIssue::cellAddress, TemplateIssue::name, TemplateIssue::type)
				.containsExactly(tuple("B2", "pointz", TemplateIssueType.UNKNOWN_ROOT));
		}

		@Test
		void 프로퍼티_오타는_실패_지점까지의_경로로_잡는다() {
			assertThat(find(cell("B3", "plan.clientNmae"), cell("C3", "plan.pitotTube.coefficients[0].velocty")))
				.extracting(TemplateIssue::cellAddress, TemplateIssue::name, TemplateIssue::type)
				.containsExactly(
					tuple("B3", "plan.clientNmae", TemplateIssueType.UNKNOWN_PROPERTY),
					tuple("C3", "plan.pitotTube.coefficients.0.velocty", TemplateIssueType.UNKNOWN_PROPERTY));
		}

		@Test
		void custom_아래_키는_정의와_대조한다() {
			assertThat(find(cell("D2", "custom.siteCod"), cell("D3", "custom['nope']")))
				.extracting(TemplateIssue::name, TemplateIssue::type)
				.containsExactly(
					tuple("custom.siteCod", TemplateIssueType.UNKNOWN_CUSTOM_KEY),
					tuple("custom.nope", TemplateIssueType.UNKNOWN_CUSTOM_KEY));
		}

		@Test
		void 문법_오류는_PARSE_ERROR() {
			assertThat(find(cell("B2", "plan.name +")))
				.extracting(TemplateIssue::name, TemplateIssue::type)
				.containsExactly(tuple("plan.name +", TemplateIssueType.PARSE_ERROR));
		}

		@Test
		void 한_표현식의_여러_변수를_각각_판정한다() {
			assertThat(find(cell("B2", "plan.stackName + plan.stackNmae + custom.nope")))
				.extracting(TemplateIssue::name)
				.containsExactlyInAnyOrder("plan.stackNmae", "custom.nope");
		}
	}

	@Nested
	@DisplayName("반복 변수")
	class LoopVariables {

		@Test
		void 반복_변수는_items_의_원소_타입에_묶인다() {
			List<TemplateIssue> issues = find(each("A6", "points", "p"), cell("A6", "p.ts"), cell("B6", "p.tz"));

			assertThat(issues)
				.extracting(TemplateIssue::cellAddress, TemplateIssue::name, TemplateIssue::type)
				.containsExactly(tuple("B6", "p.tz", TemplateIssueType.UNKNOWN_PROPERTY));
		}

		@Test
		void 중첩_반복도_바깥_변수를_따라_묶인다() {
			List<TemplateIssue> issues = find(
				each("A6", "plan.equipments", "e"),
				each("B6", "e.inspections", "ins"),
				cell("B6", "ins.nextDueDate"), cell("C6", "ins.nextDueDat"));

			assertThat(issues).extracting(TemplateIssue::name).containsExactly("ins.nextDueDat");
		}

		/** 바깥 반복 메모가 안쪽보다 뒤에 와도(순서가 뒤바뀐 중첩) 되돌아 풀어낸다. */
		@Test
		void 반복_메모_순서가_뒤바뀌어도_풀린다() {
			List<TemplateIssue> issues = find(
				each("B6", "e.inspections", "ins"),
				each("A6", "plan.equipments", "e"),
				cell("B6", "ins.nextDueDat"));

			assertThat(issues).extracting(TemplateIssue::name).containsExactly("ins.nextDueDat");
		}

		/** items 오타는 그것만 잡고, 그 반복 변수 아래는 연쇄 오탐을 내지 않는다. */
		@Test
		void items_가_풀리지_않으면_그_반복_변수_아래는_검사하지_않는다() {
			List<TemplateIssue> issues = find(each("A6", "pointz", "p"), cell("A6", "p.anything.at.all"));

			assertThat(issues)
				.extracting(TemplateIssue::source, TemplateIssue::name, TemplateIssue::type)
				.containsExactly(tuple(Source.COMMENT, "pointz", TemplateIssueType.UNKNOWN_ROOT));
		}

		@Test
		void varIndex_는_정수로_묶인다() {
			TemplateExpressionRef each = ref("A6", Source.COMMENT, "each",
				Map.of("items", "points", "var", "p", "varIndex", "i"), "points");

			assertThat(find(each, cell("A6", "i + 1"), cell("B6", "i.nope"))).extracting(TemplateIssue::name)
				.containsExactly("i.nope");
		}
	}

	@Nested
	@DisplayName("시트 단위")
	class SheetLevel {

		@Test
		void 표현식이_있는데_area_가_없으면_AREA_MISSING() {
			List<TemplateIssue> issues = finder.find(List.of(cell("B2", "plan.stackName")), CUSTOM_KEYS);

			assertThat(issues)
				.extracting(TemplateIssue::sheetName, TemplateIssue::cellAddress, TemplateIssue::type)
				.containsExactly(tuple("Record", null, TemplateIssueType.AREA_MISSING));
		}

		@Test
		void 표현식이_없는_시트는_area_가_없어도_문제가_아니다() {
			assertThat(finder.find(List.of(), CUSTOM_KEYS)).isEmpty();
		}

		@Test
		void 시트별로_따로_판정한다() {
			TemplateExpressionRef other = new TemplateExpressionRef("Notes", "A1", Source.CELL, null, Map.of(),
				"custom.memo", List.of(List.of("custom", "memo")), true);

			List<TemplateIssue> issues = finder.find(List.of(area("A1"), cell("B2", "plan.stackName"), other), CUSTOM_KEYS);

			assertThat(issues)
				.extracting(TemplateIssue::sheetName, TemplateIssue::type)
				.containsExactlyInAnyOrder(
					tuple("Notes", TemplateIssueType.AREA_MISSING),
					tuple("Notes", TemplateIssueType.UNKNOWN_CUSTOM_KEY));
		}
	}
}
