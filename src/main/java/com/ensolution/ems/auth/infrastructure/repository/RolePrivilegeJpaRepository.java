package com.ensolution.ems.auth.infrastructure.repository;

import com.ensolution.ems.auth.infrastructure.entity.RolePrivilegeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePrivilegeJpaRepository extends JpaRepository<RolePrivilegeEntity, Long> {

	List<RolePrivilegeEntity> findByRole_RoleId(Long roleId);

	boolean existsByRole_RoleIdAndPrivilege_PrivilegeId(Long roleId, Long privilegeId);
}
