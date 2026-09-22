package com.ensolution.ems.schedule.application.command.update;

import java.util.Map;

/**
 * 회차 커스텀 필드 값 일괄 저장 파라미터. <b>전체 채택</b>이다 — 커스텀 필드 폼이 단독 소유하는 경로라
 * 폼이 정의된 필드 전부를 보내며, 요청에 없는 키와 빈 값은 "지웠다"로 읽는다.
 *
 * @param values 키 = 이 tenant의 {@code CustomFieldDefinition.key}, 값 = 템플릿이 {@code ${custom.<key>}}로 읽을 문자열
 */
public record SaveScheduleCustomFieldsCommand(Map<String, String> values) {}
