package com.dodo.todo.auth.domain;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            delete from refresh_token
            where member_id = :memberId
              and id not in (
                select id
                from (
                  select id
                  from refresh_token
                  where member_id = :memberId
                  order by updated_at desc, id desc
                  limit :retainCount
                ) latest_refresh_tokens
              )
            """, nativeQuery = true)
    int deleteOldTokensKeepingLatest(
            @Param("memberId") Long memberId,
            @Param("retainCount") int retainCount
    );
}
