package com.ensolution.ems.storage.infrastructure.repository;

import com.ensolution.ems.storage.infrastructure.entity.DocumentVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentVersionJpaRepository extends JpaRepository<DocumentVersionEntity, Long> {

	Optional<DocumentVersionEntity> findByDocument_DocumentIdAndVersionNo(Long documentId, int versionNo);

	List<DocumentVersionEntity> findAllByDocument_DocumentIdOrderByVersionNoDesc(Long documentId);

	@Query("select v.storageKey from DocumentVersionEntity v where v.document.documentId = :documentId")
	List<String> findStorageKeysByDocumentId(@Param("documentId") Long documentId);

	long countByDocument_DocumentId(Long documentId);

	@Query("select coalesce(max(v.versionNo), 0) from DocumentVersionEntity v where v.document.documentId = :documentId")
	int findMaxVersionNoByDocumentId(@Param("documentId") Long documentId);

	@Modifying
	@Query("delete from DocumentVersionEntity v where v.document.documentId = :documentId and v.versionNo = :versionNo")
	int deleteByDocumentIdAndVersionNo(@Param("documentId") Long documentId, @Param("versionNo") int versionNo);

	@Modifying
	@Query("delete from DocumentVersionEntity v where v.document.documentId = :documentId")
	int deleteAllByDocumentId(@Param("documentId") Long documentId);
}
