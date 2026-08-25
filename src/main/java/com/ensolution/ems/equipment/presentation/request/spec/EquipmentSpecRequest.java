package com.ensolution.ems.equipment.presentation.request.spec;

/**
 * 장비 유형별 사양 요청 DTO의 공통 타입.
 * 다형성 역직렬화는 이를 참조하는 요청 DTO(spec 필드)의
 * {@code @JsonTypeInfo(EXTERNAL_PROPERTY = "type")} 로 처리합니다.
 */
public sealed interface EquipmentSpecRequest
	permits ParticleSamplerSpecRequest, GasSamplerSpecRequest, PitotTubeSpecRequest,
	GasAnalyzerSpecRequest, NozzleSpecRequest, OtherSpecRequest {
}
