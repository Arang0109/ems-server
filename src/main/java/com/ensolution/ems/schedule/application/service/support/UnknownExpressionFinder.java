package com.ensolution.ems.schedule.application.service.support;

import com.ensolution.ems.schedule.application.command.export.SamplingRecordVariable;
import com.ensolution.ems.schedule.application.command.export.TemplateExpressionRef;
import com.ensolution.ems.schedule.application.command.export.TemplateExpressionRef.Source;
import com.ensolution.ems.schedule.application.command.export.TemplateIssue;
import com.ensolution.ems.schedule.application.command.export.TemplateIssueType;
import org.springframework.stereotype.Component;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 템플릿 표현식 가운데 바인딩 계약에 없는 이름을 찾는다. 렌더러는 없는 이름을 예외 없이 빈칸으로 넘기므로
 * 이 검사가 오타를 잡는 유일한 자리다.
 *
 * <p><b>알려진 이름은 리플렉션으로 계산한다.</b> 최상위 변수는 {@link SamplingRecordVariable}에서, 그 아래 프로퍼티는
 * {@code ~ExportView} 클래스의 getter({@link Introspector})에서 읽는다 — JEXL이 프로퍼티를 해석하는 규칙과 같다.
 * 목록으로 수작업 복제하면 뷰에 필드가 늘 때마다 어긋난다.
 *
 * <p>시트마다 두 번 훑는다. ① {@code jx:each}의 {@code items}를 풀어 반복 변수({@code var}·{@code varIndex})의 타입을
 * 묶는다 — 실제 오타의 대부분이 {@code ${p.temperture}}처럼 반복 변수 아래에서 나기 때문이다. 바깥 반복이 먼저
 * 오도록 리더가 행·열 순으로 넘기지만, 순서가 뒤바뀐 중첩도 풀 수 있게 새 바인딩이 생기지 않을 때까지 되돈다.
 * {@code items} 자체가 풀리지 않으면 그것만 문제로 내고 반복 변수는 와일드카드로 둔다(연쇄 오탐 방지).
 * ② 모든 표현식의 변수 경로를 타입 트리로 따라간다.
 *
 * <p>반복 변수의 유효 범위({@code lastCell} 영역)는 따지지 않는다 — 영역 밖에서 반복 변수를 쓰면 jxls도 빈칸일 뿐
 * 실패하지 않으므로 거짓 통과의 대가가 작다. 반대로 시트에 {@code ${}}가 있는데 {@code jx:area}가 없으면 그 표현식은
 * 평가조차 되지 않아 원문이 남으므로 시트 단위 문제로 낸다.
 */
@Component
public class UnknownExpressionFinder {

	private static final String CUSTOM = SamplingRecordVariable.CUSTOM.getVariableName();

	/** 어떤 하위 경로든 통과시키는 표식 — 출처가 풀리지 않은 반복 변수, 원소 타입을 모르는 목록. */
	private static final Binding WILDCARD = new Binding(Object.class, null, true);

	private final Map<Class<?>, Map<String, Binding>> propertyCache = new ConcurrentHashMap<>();

	public List<TemplateIssue> find(List<TemplateExpressionRef> refs, Set<String> customKeys) {
		Map<String, List<TemplateExpressionRef>> bySheet = new LinkedHashMap<>();
		for (TemplateExpressionRef ref : refs) {
			bySheet.computeIfAbsent(ref.sheetName(), k -> new ArrayList<>()).add(ref);
		}

		List<TemplateIssue> issues = new ArrayList<>();
		bySheet.forEach((sheetName, sheetRefs) -> findInSheet(sheetName, sheetRefs, customKeys, issues));
		return issues;
	}

	private void findInSheet(String sheetName, List<TemplateExpressionRef> refs, Set<String> customKeys,
	                         List<TemplateIssue> issues) {
		boolean hasCellExpression = refs.stream().anyMatch(r -> r.source() == Source.CELL);
		boolean hasArea = refs.stream().anyMatch(r -> r.isCommand("area"));
		if (hasCellExpression && !hasArea) {
			issues.add(new TemplateIssue(sheetName, null, null, null, "jx:area", TemplateIssueType.AREA_MISSING));
		}

		Map<String, Binding> loopVariables = bindLoopVariables(refs, customKeys);

		for (TemplateExpressionRef ref : refs) {
			if (ref.expression() == null) continue;
			if (!ref.parsable()) {
				issues.add(issue(ref, ref.expression(), TemplateIssueType.PARSE_ERROR));
				continue;
			}
			for (List<String> path : ref.variablePaths()) {
				Resolution resolution = resolve(path, loopVariables, customKeys);
				if (!resolution.ok()) {
					issues.add(issue(ref, resolution.failedName(), resolution.type()));
				}
			}
		}
	}

	/** ① 반복 변수 바인딩. 새로 묶인 것이 없을 때까지 되돈다(순서가 뒤바뀐 중첩 대응, 상한은 each 개수). */
	private Map<String, Binding> bindLoopVariables(List<TemplateExpressionRef> refs, Set<String> customKeys) {
		Map<String, Binding> bound = new HashMap<>();
		List<TemplateExpressionRef> eachRefs = refs.stream().filter(r -> r.isCommand("each")).toList();
		Set<String> declaredVars = eachRefs.stream()
			.map(r -> r.attribute("var")).filter(v -> v != null).collect(Collectors.toSet());

		boolean progressed = true;
		for (int round = 0; progressed && round <= eachRefs.size(); round++) {
			progressed = false;
			for (TemplateExpressionRef each : eachRefs) {
				String var = each.attribute("var");
				if (var == null || bound.containsKey(var)) continue;

				Binding items = itemsBinding(each, bound, declaredVars, customKeys);
				if (items == null) continue;   // 아직 바깥 반복 변수가 묶이지 않았다 — 다음 회차에

				bound.put(var, items.elementBinding());
				String varIndex = each.attribute("varIndex");
				if (varIndex != null) bound.put(varIndex, new Binding(Integer.class, null, false));
				progressed = true;
			}
		}
		// 끝까지 풀리지 않은 반복 변수(items 오타 등)는 와일드카드 — items 자체는 ②에서 문제로 잡힌다.
		for (TemplateExpressionRef each : eachRefs) {
			String var = each.attribute("var");
			if (var != null) bound.putIfAbsent(var, WILDCARD);
			String varIndex = each.attribute("varIndex");
			if (varIndex != null) bound.putIfAbsent(varIndex, new Binding(Integer.class, null, false));
		}
		return bound;
	}

	/**
	 * {@code items} 표현식이 가리키는 타입. 루트가 아직 묶이지 않은 바깥 반복 변수면 null(다음 회차에 다시),
	 * 풀리지 않는 경로(오타)면 와일드카드 — {@code items} 자체는 ②에서 문제로 잡힌다.
	 */
	private Binding itemsBinding(TemplateExpressionRef each, Map<String, Binding> bound, Set<String> declaredVars,
	                             Set<String> customKeys) {
		if (!each.parsable() || each.variablePaths().size() != 1) return WILDCARD;
		List<String> path = each.variablePaths().get(0);
		String root = path.get(0);
		if (!bound.containsKey(root) && declaredVars.contains(root)) return null;

		Resolution resolution = resolve(path, bound, customKeys);
		return resolution.ok() ? resolution.binding() : WILDCARD;
	}

	/** ② 경로를 타입 트리로 따라간다. */
	private Resolution resolve(List<String> path, Map<String, Binding> loopVariables, Set<String> customKeys) {
		String root = path.get(0);
		Binding current = loopVariables.get(root);
		if (current == null) {
			current = SamplingRecordVariable.byName(root)
				.map(v -> new Binding(v.getType(), v.getElementType(), false))
				.orElse(null);
		}
		if (current == null) {
			return Resolution.fail(root, TemplateIssueType.UNKNOWN_ROOT);
		}

		for (int i = 1; i < path.size(); i++) {
			String segment = path.get(i);
			String soFar = String.join(".", path.subList(0, i + 1));

			if (current.wildcard()) return Resolution.ok(WILDCARD);

			if (Map.class.isAssignableFrom(current.type())) {
				if (CUSTOM.equals(root) && i == 1) {
					if (!customKeys.contains(segment)) {
						return Resolution.fail(soFar, TemplateIssueType.UNKNOWN_CUSTOM_KEY);
					}
					current = new Binding(String.class, null, false);
					continue;
				}
				current = WILDCARD;   // 일반 Map은 키를 알 수 없다
				continue;
			}

			if (Collection.class.isAssignableFrom(current.type())) {
				if (isIndex(segment)) {
					current = current.elementBinding();
					continue;
				}
				Binding property = propertyOf(current.type(), segment);
				if (property == null) return Resolution.fail(soFar, TemplateIssueType.UNKNOWN_PROPERTY);
				current = property;
				continue;
			}

			Binding property = propertyOf(current.type(), segment);
			if (property == null) return Resolution.fail(soFar, TemplateIssueType.UNKNOWN_PROPERTY);
			current = property;
		}
		return Resolution.ok(current);
	}

	private static boolean isIndex(String segment) {
		return !segment.isEmpty() && segment.chars().allMatch(Character::isDigit);
	}

	/** JEXL과 같은 규칙 — 표준 getter({@code getX}·{@code isX}). {@code class}는 값이 아니라 제외한다. */
	private Binding propertyOf(Class<?> type, String name) {
		return propertyCache.computeIfAbsent(type, this::introspect).get(name);
	}

	private Map<String, Binding> introspect(Class<?> type) {
		Map<String, Binding> properties = new HashMap<>();
		try {
			BeanInfo info = Introspector.getBeanInfo(type);
			for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {
				Method getter = descriptor.getReadMethod();
				if (getter == null || "class".equals(descriptor.getName())) continue;
				properties.put(descriptor.getName(), bindingOf(getter));
			}
		} catch (IntrospectionException e) {
			// 인트로스펙션이 실패하는 타입은 하위 경로를 검사하지 않는다
		}
		return properties;
	}

	private static Binding bindingOf(Method getter) {
		Class<?> type = getter.getReturnType();
		if (!Collection.class.isAssignableFrom(type)) return new Binding(type, null, false);

		Type generic = getter.getGenericReturnType();
		if (generic instanceof ParameterizedType parameterized
			&& parameterized.getActualTypeArguments()[0] instanceof Class<?> element) {
			return new Binding(type, element, false);
		}
		return new Binding(type, null, false);
	}

	private static TemplateIssue issue(TemplateExpressionRef ref, String name, TemplateIssueType type) {
		return new TemplateIssue(ref.sheetName(), ref.cellAddress(), ref.source(), ref.expression(), name, type);
	}

	/** 경로 위 한 지점의 타입. 목록이면 원소 타입을 함께 든다. */
	private record Binding(Class<?> type, Class<?> elementType, boolean wildcard) {
		Binding elementBinding() {
			return elementType == null ? WILDCARD : new Binding(elementType, null, false);
		}
	}

	private record Resolution(Binding binding, String failedName, TemplateIssueType type) {
		static Resolution ok(Binding binding) { return new Resolution(binding, null, null); }
		static Resolution fail(String name, TemplateIssueType type) { return new Resolution(null, name, type); }
		boolean ok() { return type == null; }
	}
}
