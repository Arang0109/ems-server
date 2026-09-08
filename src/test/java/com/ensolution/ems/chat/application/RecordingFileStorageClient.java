package com.ensolution.ems.chat.application;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.global.storage.FileStorageClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 보관소 호출을 기록하는 {@link FileStorageClient}.
 * {@code DocumentServiceTest}의 {@code RecordingFileStorage}와 같은 형태다.
 *
 * <p>실물이 없을 때 실제 어댑터와 같은 {@code STORAGE_FILE_NOT_FOUND}를 던진다 —
 * 파일이 사라진 경우의 응답이 테스트에서 달라지면 회귀를 놓친다.
 */
public class RecordingFileStorageClient implements FileStorageClient {

	private final Map<String, byte[]> stored = new LinkedHashMap<>();

	public Map<String, byte[]> stored() {
		return stored;
	}

	/** 실물이 사라진 상황을 만든다(보관소 장애·수동 삭제). */
	public void evict(String storageKey) {
		stored.remove(storageKey);
	}

	@Override
	public void store(String storageKey, byte[] content) {
		stored.put(storageKey, content);
	}

	@Override
	public byte[] load(String storageKey) {
		byte[] content = stored.get(storageKey);
		if (content == null) {
			throw new CustomException(ErrorCode.STORAGE_FILE_NOT_FOUND);
		}
		return content;
	}

	@Override
	public void delete(String storageKey) {
		stored.remove(storageKey);
	}
}
