package com.ensolution.ems.auth.application.port.in;

public interface UserCommandUseCase {
	void createUser(CreateUserCommand command);
	void updateUser(UpdateUserCommand command);
	void deleteUser(Long userId);
}
