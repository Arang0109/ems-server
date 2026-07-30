package com.ensolution.ems.storage.application.port.out;

import com.ensolution.ems.storage.domain.StorageProvider;

/**
 * 파일 실물의 보관소. 구현체를 교체해 로컬 디스크·S3 등으로 저장 위치를 바꾼다.
 * 현재는 {@code LocalFileStorageAdapter}만 존재한다.
 */
public interface FileStorageClient {

	void store(String storageKey, byte[] content);

	byte[] load(String storageKey);

	/** 대상이 이미 없어도 예외를 던지지 않는다. */
	void delete(String storageKey);

	/** 이 구현체가 저장한 파일에 기록될 보관소 종류. */
	StorageProvider provider();
}
