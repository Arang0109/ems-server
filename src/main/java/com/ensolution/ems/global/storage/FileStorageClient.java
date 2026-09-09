package com.ensolution.ems.global.storage;

/**
 * 파일 실물의 보관소. <b>기능 모듈이 아니라 공통 인프라</b>이므로 {@code global}에 둔다 —
 * {@code store}·{@code load}·{@code delete} 셋뿐이며 업무 규칙이 한 줄도 없다.
 * <p>
 * 구현체는 환경마다 하나만 등록된다({@code ems.storage.provider}). 소비자는 어느 보관소인지 모른 채
 * {@code storageKey}만 넘기고, 키를 해석하는 방식은 구현체의 몫이다.
 * <p>
 * <b>{@code storageKey}는 소비 모듈의 도메인이 만든다.</b> 구현체는 키를 만들지 않는다
 * ({@code storage}의 {@code DocumentVersion}, {@code chat}의 {@code ChatAttachment}).
 * 사용자 입력이 경로에 섞이지 않도록 UUID로 만들며, 그래서 S3 어댑터는 경로 이탈 검사를 두지 않는다.
 * <p>
 * 현재 소비자는 {@code storage}(문서 버전)와 {@code chat}(대화 첨부) 두 모듈이다.
 */
public interface FileStorageClient {

	void store(String storageKey, byte[] content);

	byte[] load(String storageKey);

	/** 대상이 이미 없어도 예외를 던지지 않는다. */
	void delete(String storageKey);
}
