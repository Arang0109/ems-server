package com.ensolution.ems.schedule.application.command.update;

import java.util.List;

/**
 * 측정계획의 측정장비 교체 커맨드.
 *
 * <p>전달된 목록으로 <b>전체 교체</b>한다(부분 갱신이 아니다). 장비 유형은 장비 원장이 알고 있으므로
 * 유형별 슬롯을 지정하지 않으며, 빈 목록은 "장비 없음"을 뜻한다.
 */
public record ChangeScheduleEquipmentsCommand(List<String> equipmentIds) {}
