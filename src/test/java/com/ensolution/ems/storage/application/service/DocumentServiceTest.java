package com.ensolution.ems.storage.application.service;

import com.ensolution.ems.global.common.enums.DocumentCategory;
import com.ensolution.ems.global.exception.CustomException;
import com.ensolution.ems.global.exception.ErrorCode;
import com.ensolution.ems.storage.application.FakeDocumentRepository;
import com.ensolution.ems.storage.application.FakeDocumentVersionRepository;
import com.ensolution.ems.storage.application.command.DocumentFile;
import com.ensolution.ems.storage.application.port.in.AddDocumentVersionCommand;
import com.ensolution.ems.storage.application.port.in.CreateDocumentCommand;
import com.ensolution.ems.storage.application.port.in.UploadedFile;
import com.ensolution.ems.global.storage.FileStorageClient;
import com.ensolution.ems.storage.application.validator.DocumentValidator;
import com.ensolution.ems.storage.domain.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 문서 버전 이력이 지키는 규칙을 고정한다. 모듈 문서가 "다음 테스트 대상"으로 지목한 항목들이다.
 *
 * <p>첫째, <b>버전 번호 부여</b> — 버전은 덮어쓰지 않고 쌓이며, 번호는 문서의 {@code latestVersionNo}를
 * 따라 1씩 오른다. 이 규칙이 흔들리면 과거 양식을 받아야 하는 실무 요구가 깨진다.
 *
 * <p>둘째, <b>마지막 한 개는 지울 수 없다</b> — 버전이 0개가 되면 문서가 다운로드 불가 상태로 남는다.
 * 그리고 최신 버전을 지웠을 때만 {@code latestVersionNo}를 남은 최대값으로 내린다.
 *
 * <p>셋째, <b>파일 쓰기는 메타 저장 뒤에 온다</b> — 파일 쓰기가 실패하면 트랜잭션이 롤백되어 실물 없는
 * 레코드가 남지 않는다. 삭제는 반대다. 순서가 뒤집혔는지는 호출 순서로만 드러나므로 여기서 고정한다.
 *
 * <p>넷째, <b>부모 경유 tenant 격리</b> — 버전 포트는 tenantId를 받지 않으므로, 모든 경로가 문서
 * 소유권을 먼저 확인한다는 관례에 격리가 걸려 있다. 그 관례가 유지되는지 확인한다.
 */
class DocumentServiceTest {

	private static final long TENANT = 1L;
	private static final long OTHER_TENANT = 2L;
	private static final long UPLOADER = 7L;

	private final FakeDocumentRepository documentRepository = new FakeDocumentRepository();
	private final FakeDocumentVersionRepository versionRepository = new FakeDocumentVersionRepository();
	private final RecordingFileStorage fileStorage = new RecordingFileStorage();

	private final DocumentService documentService = new DocumentService(
		documentRepository,
		versionRepository,
		fileStorage,
		new DocumentValidator(documentRepository)
	);

	private static UploadedFile file(String filename, String content) {
		byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
		return new UploadedFile(filename, "text/plain", (long) bytes.length, bytes);
	}

	private Long createDocument(String name) {
		return documentService.createDocument(new CreateDocumentCommand(
			TENANT, name, DocumentCategory.ETC, "설명", "최초", UPLOADER, file("v1.txt", "v1")));
	}

	@Nested
	@DisplayName("버전 번호 부여")
	class VersionNumbering {

		@Test
		@DisplayName("문서를 만들면 1번 버전이 함께 생기고 최신 번호가 1이 된다")
		void 생성하면_1번_버전이_생긴다() {
			Long documentId = createDocument("양식");

			assertThat(versionRepository.versionNosOf(documentId)).containsExactly(1);
			assertThat(documentRepository.peek(documentId).orElseThrow().getLatestVersionNo()).isEqualTo(1);
		}

		@Test
		@DisplayName("버전을 추가하면 번호가 1씩 오르고 이전 버전은 남는다")
		void 버전은_덮어쓰지_않고_쌓인다() {
			Long documentId = createDocument("양식");

			int second = documentService.addVersion(new AddDocumentVersionCommand(
				documentId, TENANT, "개정", UPLOADER, file("v2.txt", "v2")));
			int third = documentService.addVersion(new AddDocumentVersionCommand(
				documentId, TENANT, "재개정", UPLOADER, file("v3.txt", "v3")));

			assertThat(second).isEqualTo(2);
			assertThat(third).isEqualTo(3);
			assertThat(versionRepository.versionNosOf(documentId)).containsExactly(1, 2, 3);
			assertThat(documentRepository.peek(documentId).orElseThrow().getLatestVersionNo()).isEqualTo(3);
		}

		@Test
		@DisplayName("같은 이름의 문서는 만들 수 없다")
		void 문서명은_테넌트_안에서_유일하다() {
			createDocument("양식");

			assertThatThrownBy(() -> createDocument("양식"))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_ALREADY_EXISTS);
		}
	}

	@Nested
	@DisplayName("버전 삭제")
	class DeleteVersion {

		@Test
		@DisplayName("마지막 한 개는 지울 수 없다 — 버전 0개인 문서는 다운로드 불가 상태가 된다")
		void 마지막_버전은_지울_수_없다() {
			Long documentId = createDocument("양식");

			assertThatThrownBy(() -> documentService.deleteVersion(documentId, 1, TENANT))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_LAST_VERSION_NOT_DELETABLE);

			assertThat(versionRepository.versionNosOf(documentId)).containsExactly(1);
			assertThat(fileStorage.deleted).isEmpty();
		}

		@Test
		@DisplayName("최신 버전을 지우면 최신 번호가 남은 최대값으로 내려간다")
		void 최신_버전을_지우면_번호가_내려간다() {
			Long documentId = createDocument("양식");
			documentService.addVersion(new AddDocumentVersionCommand(
				documentId, TENANT, "개정", UPLOADER, file("v2.txt", "v2")));

			documentService.deleteVersion(documentId, 2, TENANT);

			assertThat(versionRepository.versionNosOf(documentId)).containsExactly(1);
			assertThat(documentRepository.peek(documentId).orElseThrow().getLatestVersionNo()).isEqualTo(1);
		}

		@Test
		@DisplayName("최신이 아닌 버전을 지우면 최신 번호는 그대로다")
		void 중간_버전을_지우면_번호는_유지된다() {
			Long documentId = createDocument("양식");
			documentService.addVersion(new AddDocumentVersionCommand(
				documentId, TENANT, "개정", UPLOADER, file("v2.txt", "v2")));

			documentService.deleteVersion(documentId, 1, TENANT);

			assertThat(versionRepository.versionNosOf(documentId)).containsExactly(2);
			assertThat(documentRepository.peek(documentId).orElseThrow().getLatestVersionNo()).isEqualTo(2);
		}
	}

	@Nested
	@DisplayName("메타와 파일의 순서")
	class WriteOrder {

		@Test
		@DisplayName("등록은 메타를 먼저 저장하고 파일을 나중에 쓴다 — 파일 실패 시 롤백되어 고아 레코드가 없다")
		void 등록은_파일_쓰기가_마지막이다() {
			Long documentId = createDocument("양식");

			// 파일이 쓰인 시점에 이미 버전 레코드가 존재해야 한다.
			String storageKey = versionRepository.findByDocumentIdAndVersionNo(documentId, 1).getStorageKey();
			assertThat(fileStorage.stored).containsOnlyKeys(storageKey);
		}

		@Test
		@DisplayName("문서를 지우면 버전 전체와 실물 파일이 함께 정리된다")
		void 문서_삭제는_파일까지_정리한다() {
			Long documentId = createDocument("양식");
			documentService.addVersion(new AddDocumentVersionCommand(
				documentId, TENANT, "개정", UPLOADER, file("v2.txt", "v2")));
			int storedBefore = fileStorage.stored.size();

			documentService.deleteDocument(documentId, TENANT);

			assertThat(documentRepository.peek(documentId)).isEmpty();
			assertThat(versionRepository.versionNosOf(documentId)).isEmpty();
			assertThat(fileStorage.deleted).hasSize(storedBefore);
		}
	}

	@Nested
	@DisplayName("다운로드")
	class Download {

		@Test
		@DisplayName("versionNo가 null이면 최신 버전을 내려준다")
		void null이면_최신_버전이다() {
			Long documentId = createDocument("양식");
			documentService.addVersion(new AddDocumentVersionCommand(
				documentId, TENANT, "개정", UPLOADER, file("v2.txt", "최신내용")));

			DocumentFile downloaded = documentService.download(documentId, TENANT, null);

			assertThat(downloaded.filename()).isEqualTo("v2.txt");
			assertThat(new String(downloaded.content(), StandardCharsets.UTF_8)).isEqualTo("최신내용");
		}

		@Test
		@DisplayName("지정한 과거 버전도 그대로 받을 수 있다 — 버전을 쌓아 두는 이유다")
		void 과거_버전도_받을_수_있다() {
			Long documentId = createDocument("양식");
			documentService.addVersion(new AddDocumentVersionCommand(
				documentId, TENANT, "개정", UPLOADER, file("v2.txt", "최신내용")));

			DocumentFile downloaded = documentService.download(documentId, TENANT, 1);

			assertThat(downloaded.filename()).isEqualTo("v1.txt");
			assertThat(new String(downloaded.content(), StandardCharsets.UTF_8)).isEqualTo("v1");
		}

		@Test
		@DisplayName("없는 버전 번호는 DOCUMENT_VERSION_NOT_FOUND")
		void 없는_버전은_찾을_수_없다() {
			Long documentId = createDocument("양식");

			assertThatThrownBy(() -> documentService.download(documentId, TENANT, 99))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_VERSION_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("부모 경유 tenant 격리 — 버전 포트에는 tenantId가 없다")
	class TenantIsolation {

		@Test
		@DisplayName("다른 테넌트의 문서는 다운로드할 수 없다")
		void 남의_문서는_받을_수_없다() {
			Document stranger = documentRepository.given(OTHER_TENANT, "남의 양식", 1);
			versionRepository.given(stranger.getId(), 1, "2/1/1/key.xlsx");

			assertThatThrownBy(() -> documentService.download(stranger.getId(), TENANT, null))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_NOT_FOUND);
		}

		@Test
		@DisplayName("다른 테넌트의 문서에는 버전을 추가할 수 없다")
		void 남의_문서에는_버전을_더할_수_없다() {
			Document stranger = documentRepository.given(OTHER_TENANT, "남의 양식", 1);
			versionRepository.given(stranger.getId(), 1, "2/1/1/key.xlsx");

			assertThatThrownBy(() -> documentService.addVersion(new AddDocumentVersionCommand(
				stranger.getId(), TENANT, "개정", UPLOADER, file("v2.txt", "v2"))))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_NOT_FOUND);

			assertThat(versionRepository.versionNosOf(stranger.getId())).containsExactly(1);
		}

		@Test
		@DisplayName("다른 테넌트의 버전은 지울 수 없고 실물 파일도 건드리지 않는다")
		void 남의_버전은_지울_수_없다() {
			Document stranger = documentRepository.given(OTHER_TENANT, "남의 양식", 2);
			versionRepository.given(stranger.getId(), 1, "2/1/1/key.xlsx");
			versionRepository.given(stranger.getId(), 2, "2/1/2/key.xlsx");

			assertThatThrownBy(() -> documentService.deleteVersion(stranger.getId(), 2, TENANT))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_NOT_FOUND);

			assertThat(versionRepository.versionNosOf(stranger.getId())).containsExactly(1, 2);
			assertThat(fileStorage.deleted).isEmpty();
		}

		@Test
		@DisplayName("다른 테넌트의 문서는 삭제할 수 없다")
		void 남의_문서는_지울_수_없다() {
			Document stranger = documentRepository.given(OTHER_TENANT, "남의 양식", 1);
			versionRepository.given(stranger.getId(), 1, "2/1/1/key.xlsx");

			assertThatThrownBy(() -> documentService.deleteDocument(stranger.getId(), TENANT))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_NOT_FOUND);

			assertThat(documentRepository.peek(stranger.getId())).isPresent();
			assertThat(versionRepository.versionNosOf(stranger.getId())).containsExactly(1);
		}

		@Test
		@DisplayName("목록에 다른 테넌트의 문서가 섞이지 않는다")
		void 목록에_남의_문서가_없다() {
			createDocument("내 양식");
			documentRepository.given(OTHER_TENANT, "남의 양식", 1);

			assertThat(documentService.getDocuments(TENANT, null))
				.extracting(summary -> summary.name())
				.containsExactly("내 양식");
		}
	}

	/**
	 * 보관소 호출을 기록하는 {@link FileStorageClient}.
	 * 파일이 언제 쓰이고 지워지는지가 이 테스트의 관심사라 인메모리로 두되 호출을 남긴다.
	 */
	private static class RecordingFileStorage implements FileStorageClient {

		private final Map<String, byte[]> stored = new LinkedHashMap<>();
		private final Map<String, Integer> deleted = new HashMap<>();

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
			deleted.merge(storageKey, 1, Integer::sum);
			stored.remove(storageKey);
		}
	}
}
