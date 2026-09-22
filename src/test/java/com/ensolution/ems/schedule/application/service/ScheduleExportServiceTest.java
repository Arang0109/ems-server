package com.ensolution.ems.schedule.application.service;

import com.ensolution.ems.schedule.application.FakeCustomFieldDefinitionRepository;
import com.ensolution.ems.schedule.application.command.export.CheckTemplateResult;
import com.ensolution.ems.schedule.application.command.export.ScheduleExportView;
import com.ensolution.ems.schedule.application.command.export.TemplateExpressionRef;
import com.ensolution.ems.schedule.application.command.export.TemplateExpressionRef.Source;
import com.ensolution.ems.schedule.application.command.export.TemplateIssue;
import com.ensolution.ems.schedule.application.command.export.TemplateIssueType;
import com.ensolution.ems.schedule.application.port.out.ExcelTemplateReader;
import com.ensolution.ems.schedule.application.port.out.SheetExcelRenderer;
import com.ensolution.ems.schedule.application.service.assembler.ScheduleExportAssembler;
import com.ensolution.ems.schedule.application.service.support.UnknownExpressionFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * 템플릿 검사 유스케이스가 <b>호출자 tenant의 커스텀 필드 정의만</b> 알려진 키로 쓰는지 고정한다.
 * 타 tenant의 정의가 섞이면 그 고객사에는 없는 키가 "있다"고 통과해 실제 렌더링에서 빈칸이 된다.
 * 렌더링·조립 협력자는 이 경로가 지나지 않는다.
 */
class ScheduleExportServiceTest {

	private static final Long TENANT = 1L;
	private static final Long OTHER_TENANT = 2L;
	private static final byte[] ANY_TEMPLATE = new byte[0];

	private static final ScheduleExportAssembler UNUSED_ASSEMBLER = new ScheduleExportAssembler(null, null, null) {
		@Override
		public ScheduleExportView assemble(Long scheduleId, Long tenantId) { throw new UnsupportedOperationException(); }
	};

	private static final SheetExcelRenderer UNUSED_RENDERER = (template, data) -> {
		throw new UnsupportedOperationException();
	};

	/** 리더는 인프라(POI)이므로 고정된 표현식 목록을 돌려주는 가짜로 대신한다. */
	private static final ExcelTemplateReader FIXED_READER = template -> List.of(
		new TemplateExpressionRef("Record", "A1", Source.COMMENT, "area", Map.of("lastCell", "C3"), null, List.of(), true),
		new TemplateExpressionRef("Record", "B2", Source.CELL, null, Map.of(), "custom.siteCode",
			List.of(List.of("custom", "siteCode")), true),
		new TemplateExpressionRef("Record", "C2", Source.CELL, null, Map.of(), "custom.otherTenantKey",
			List.of(List.of("custom", "otherTenantKey")), true),
		new TemplateExpressionRef("Record", "D2", Source.CELL, null, Map.of(), "plan.stackName",
			List.of(List.of("plan", "stackName")), true));

	private FakeCustomFieldDefinitionRepository definitions;
	private ScheduleExportService service;

	@BeforeEach
	void setUp() {
		definitions = new FakeCustomFieldDefinitionRepository();
		service = new ScheduleExportService(UNUSED_ASSEMBLER, UNUSED_RENDERER, FIXED_READER, definitions,
			new UnknownExpressionFinder());
	}

	@Test
	void 호출자_고객사의_정의만_알려진_커스텀_키로_쓴다() {
		definitions.given(TENANT, "siteCode", "현장 코드", 10);
		definitions.given(OTHER_TENANT, "otherTenantKey", "남의 키", 10);

		CheckTemplateResult result = service.checkTemplate(TENANT, ANY_TEMPLATE);

		assertThat(result.valid()).isFalse();
		assertThat(result.issues())
			.extracting(TemplateIssue::cellAddress, TemplateIssue::name, TemplateIssue::type)
			.containsExactly(tuple("C2", "custom.otherTenantKey", TemplateIssueType.UNKNOWN_CUSTOM_KEY));
	}

	@Test
	void 문제가_없으면_유효하다() {
		definitions.given(TENANT, "siteCode", "현장 코드", 10);
		definitions.given(TENANT, "otherTenantKey", "이 고객사도 정의함", 20);

		CheckTemplateResult result = service.checkTemplate(TENANT, ANY_TEMPLATE);

		assertThat(result.valid()).isTrue();
		assertThat(result.issues()).isEmpty();
	}
}
