package com.dodo.todo.auth.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByMemberIdOrderByUpdatedAtDescIdDesc(Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshToken refreshToken
            set refreshToken.token = :newToken,
                refreshToken.expiredAt = :expiredAt,
                refreshToken.updatedAt = :updatedAt
            where refreshToken.id = :refreshTokenId
              and refreshToken.token = :currentToken
            """)
    int rotateToken(
            @Param("refreshTokenId") Long refreshTokenId,
            @Param("currentToken") String currentToken,
            @Param("newToken") String newToken,
            @Param("expiredAt") LocalDateTime expiredAt,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
