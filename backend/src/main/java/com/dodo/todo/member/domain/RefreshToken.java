package com.dodo.todo.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Column(name = "refresh_token", length = 512)
    private String token;

    @Column(name = "refresh_token_expired_at")
    private LocalDateTime expiredAt;

    public RefreshToken(String token, LocalDateTime expiredAt) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("갱신 토큰 값은 필수입니다.");
        }
        if (expiredAt == null) {
            throw new IllegalArgumentException("갱신 토큰 만료 시간은 필수입니다.");
        }
        this.token = token;
        this.expiredAt = expiredAt;
    }

    /**
     * refresh token 유효성 확인
     * 저장된 토큰과 요청 토큰이 일치하고 아직 만료되지 않았는지 판단한다.
     */
    public boolean isValidRefreshToken(String token) {
        return this.token.equals(token) && !isExpired();
    }

    /**
     * refresh token 만료 여부 확인
     * 만료 시각이 현재보다 이후가 아니면 만료된 토큰으로 판단한다.
     */
    public boolean isExpired() {
        return expiredAt == null || !expiredAt.isAfter(LocalDateTime.now());
    }
}
