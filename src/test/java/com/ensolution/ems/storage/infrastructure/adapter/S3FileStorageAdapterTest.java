package com.ensolution.ems.storage.infrastructure.adapter;

import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.storage.infrastructure.config.StorageProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * S3 보관소가 지키는 두 가지를 고정한다.
 *
 * <p>첫째, <b>오브젝트 키 조합</b> — 저장소 키 앞에 prefix가 붙는다. 이 규칙이 흔들리면 이전에 올린
 * 파일을 영영 찾지 못하므로, 조합 방식을 테스트로 못박는다.
 *
 * <p>둘째, <b>실패 처리 규약이 로컬 보관소와 같다</b>는 것 — 없는 파일은 예외, 삭제 실패는 삼킴.
 * 보관소가 달라도 서비스가 보는 실패의 모양은 같아야 한다.
 */
class S3FileStorageAdapterTest {

	private static final String BUCKET = "ems-docs";
	private static final String STORAGE_KEY = "1/3/1/f1d7cd46.xlsx";

	private static StorageProperties properties(String keyPrefix) {
		return new StorageProperties(
			"./data/storage",
			new StorageProperties.S3(BUCKET, "ap-northeast-2", keyPrefix, null, false)
		);
	}

	/**
	 * 인메모리 S3. {@code S3Client}의 오퍼레이션은 전부 default 메서드라 쓰는 것만 재정의한다 —
	 * 재정의하지 않은 오퍼레이션은 호출되는 순간 실패하므로 "이 경로는 저기까지 가지 않는다"가 구조로 고정된다.
	 */
	private static class FakeS3Client implements S3Client {

		private final Map<String, byte[]> objects = new HashMap<>();
		private boolean failEveryCall = false;

		void given(String objectKey, String content) {
			objects.put(objectKey, content.getBytes(StandardCharsets.UTF_8));
		}

		void failEveryCall() {
			this.failEveryCall = true;
		}

		@Override
		public PutObjectResponse putObject(PutObjectRequest request, RequestBody body) {
			raiseIfFailing();
			assertThat(request.bucket()).isEqualTo(BUCKET);
			objects.put(request.key(), readAll(body));
			return PutObjectResponse.builder().build();
		}

		@Override
		public ResponseBytes<GetObjectResponse> getObjectAsBytes(GetObjectRequest request) {
			raiseIfFailing();
			byte[] content = objects.get(request.key());
			if (content == null) {
				throw NoSuchKeyException.builder().message("no such key: " + request.key()).build();
			}
			return ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), content);
		}

		@Override
		public DeleteObjectResponse deleteObject(DeleteObjectRequest request) {
			raiseIfFailing();
			objects.remove(request.key());
			return DeleteObjectResponse.builder().build();
		}

		@Override
		public String serviceName() {
			return "s3";
		}

		@Override
		public void close() {
		}

		boolean has(String objectKey) {
			return objects.containsKey(objectKey);
		}

		private void raiseIfFailing() {
			if (failEveryCall) {
				throw SdkClientException.create("연결할 수 없습니다");
			}
		}

		private static byte[] readAll(RequestBody body) {
			try (InputStream in = body.contentStreamProvider().newStream()) {
				return in.readAllBytes();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	@Nested
	@DisplayName("오브젝트 키 조합")
	class ObjectKey {

		private final FakeS3Client s3 = new FakeS3Client();

		@Test
		@DisplayName("저장소 키 앞에 prefix를 붙인다")
		void prefix를_앞에_붙인다() {
			new S3FileStorageAdapter(s3, properties("documents")).store(STORAGE_KEY, "내용".getBytes(StandardCharsets.UTF_8));

			assertThat(s3.has("documents/" + STORAGE_KEY)).isTrue();
		}

		@Test
		@DisplayName("prefix 끝의 슬래시가 중복되지 않는다")
		void prefix_끝의_슬래시를_중복하지_않는다() {
			new S3FileStorageAdapter(s3, properties("documents/")).store(STORAGE_KEY, "내용".getBytes(StandardCharsets.UTF_8));

			assertThat(s3.has("documents/" + STORAGE_KEY)).isTrue();
		}

		@Test
		@DisplayName("prefix가 비면 저장소 키를 그대로 쓴다")
		void prefix가_비면_저장소_키를_그대로_쓴다() {
			new S3FileStorageAdapter(s3, properties("  ")).store(STORAGE_KEY, "내용".getBytes(StandardCharsets.UTF_8));

			assertThat(s3.has(STORAGE_KEY)).isTrue();
		}

		@Test
		@DisplayName("읽기도 같은 규칙으로 키를 조합한다")
		void 읽기도_같은_키로_찾는다() {
			s3.given("documents/" + STORAGE_KEY, "내용");

			byte[] loaded = new S3FileStorageAdapter(s3, properties("documents")).load(STORAGE_KEY);

			assertThat(new String(loaded, StandardCharsets.UTF_8)).isEqualTo("내용");
		}
	}

	@Nested
	@DisplayName("실패 처리")
	class Failures {

		private final FakeS3Client s3 = new FakeS3Client();
		private final S3FileStorageAdapter store = new S3FileStorageAdapter(s3, properties("documents"));

		@Test
		@DisplayName("오브젝트가 없으면 로컬 보관소와 같은 DOCUMENT_FILE_NOT_FOUND")
		void 없는_오브젝트는_파일_없음으로_번역한다() {
			assertThatThrownBy(() -> store.load(STORAGE_KEY))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_FILE_NOT_FOUND);
		}

		@Test
		@DisplayName("통신 실패는 STORAGE_READ_FAILED로 번역한다")
		void 읽기_통신_실패를_번역한다() {
			s3.failEveryCall();

			assertThatThrownBy(() -> store.load(STORAGE_KEY))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORAGE_READ_FAILED);
		}

		@Test
		@DisplayName("쓰기 실패는 STORAGE_WRITE_FAILED로 번역한다 — 트랜잭션이 롤백되어 실물 없는 레코드가 남지 않는다")
		void 쓰기_실패를_번역한다() {
			s3.failEveryCall();

			assertThatThrownBy(() -> store.store(STORAGE_KEY, "내용".getBytes(StandardCharsets.UTF_8)))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORAGE_WRITE_FAILED);
		}

		@Test
		@DisplayName("삭제 실패는 삼킨다 — 메타는 이미 지워졌고 고아 오브젝트가 남는 편이 덜 위험하다")
		void 삭제_실패는_삼킨다() {
			s3.failEveryCall();

			assertThatCode(() -> store.delete(STORAGE_KEY)).doesNotThrowAnyException();
		}
	}
}
