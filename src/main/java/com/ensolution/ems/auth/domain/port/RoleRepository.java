package com.ensolution.ems.auth.domain.port;

import com.ensolution.ems.auth.domain.Role;

public interface RoleRepository {

    Role findById(Long id);
}
