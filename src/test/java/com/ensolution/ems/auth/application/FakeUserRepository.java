package com.ensolution.ems.auth.application;

import com.ensolution.ems.auth.domain.User;
import com.ensolution.ems.auth.application.port.out.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 사용자 쓰기 경로 검증용 인메모리 {@link UserRepository}.
 * <p>
 * <b>tenant 필터링을 실제 어댑터 그대로 재현한다.</b> {@code findById}·{@code deleteById}가
 * tenant를 무시하면 교차 테넌트 테스트가 통과해 버리므로, 이 Fake의 필터가 곧 격리 검증의 근거다.
 * 미존재·타 tenant를 구분하지 않고 빈 값/false로 돌려주는 것도 어댑터와 같다(리소스 존재 은닉).
 */
public class FakeUserRepository implements UserRepository {

	private final List<User> users = new ArrayList<>();
	private final AtomicLong sequence = new AtomicLong();

	public User given(Long tenantId, Long roleId, String username) {
		User user = User.builder()
			.id(sequence.incrementAndGet())
			.tenantId(tenantId)
			.roleId(roleId)
			.username(username)
			.password("encoded:" + username)
			.name(username)
			.build();
		users.add(user);
		return user;
	}

	public Optional<User> peek(Long id) {
		return users.stream().filter(user -> Objects.equals(id, user.getId())).findFirst();
	}

	public int count() {
		return users.size();
	}

	@Override
	public User save(User user) {
		users.removeIf(stored -> Objects.equals(stored.getId(), user.getId()) && user.getId() != null);

		User saved = user.getId() == null
			? user.toBuilder().id(sequence.incrementAndGet()).build()
			: user;
		users.add(saved);
		return saved;
	}

	@Override
	public Optional<User> findById(Long id, Long tenantId) {
		return users.stream()
			.filter(user -> Objects.equals(id, user.getId()))
			.filter(user -> Objects.equals(tenantId, user.getTenantId()))
			.findFirst();
	}

	@Override
	public List<User> findAll(Long tenantId) {
		return users.stream()
			.filter(user -> Objects.equals(tenantId, user.getTenantId()))
			.sorted(Comparator.comparing(User::getId))
			.toList();
	}

	@Override
	public Optional<User> findByUsername(String username) {
		return users.stream()
			.filter(user -> Objects.equals(username, user.getUsername()))
			.findFirst();
	}

	@Override
	public boolean existsByUsername(String username) {
		return findByUsername(username).isPresent();
	}

	@Override
	public boolean deleteById(Long id, Long tenantId) {
		return users.removeIf(user ->
			Objects.equals(id, user.getId()) && Objects.equals(tenantId, user.getTenantId()));
	}
}
