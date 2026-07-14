package com.ensolution.ems.equipment.domain.spec;

/**
 * 장비 유형별 사양(spec) 값 객체의 공통 타입.
 * 순수 도메인 계층이므로 어떤 프레임워크에도 의존하지 않습니다.
 * 영속(Mongo) 시에는 Spring Data MongoDB가 {@code _class} 판별자로 구현체를 복원합니다.
 */
public sealed interface EquipmentSpec
	permits ParticleSamplerSpec, GasSamplerSpec, PitotTubeSpec, NozzleSpec, OtherSpec {
}
