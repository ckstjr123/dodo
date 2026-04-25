package com.dodo.todo.auth.repository;

import com.dodo.todo.auth.domain.RefreshToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByMember_IdOrderByUpdatedAtDescIdDesc(Long memberId);
}
