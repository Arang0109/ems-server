package com.ensolution.ems.storage.application.port.out;

/**
 * 파일 실물의 보관소.
 * <p>
 * 구현체는 환경마다 하나만 등록된다({@code ems.storage.provider}). 서비스는 어느 보관소인지 모른 채
 * {@code storageKey}만 넘기고, 키를 해석하는 방식은 구현체의 몫이다.
 * <p>
 * {@code storageKey}는 도메인({@code DocumentVersion})이 만든다. 구현체는 키를 만들지 않는다.
 */
public interface FileStorageClient {

	void store(String storageKey, byte[] content);

	byte[] load(String storageKey);

	/** 대상이 이미 없어도 예외를 던지지 않는다. */
	void delete(String storageKey);
}
