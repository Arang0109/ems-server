package com.ensolution.ems.auth.domain.port;

import com.ensolution.ems.auth.domain.Role;

import java.util.List;

public interface RoleRepository {
		List<Role> findAll();

    Role findById(Long id);
}
