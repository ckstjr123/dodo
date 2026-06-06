package com.dodo.todo.member.domain;

import com.dodo.todo.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Embedded
    private RefreshToken refreshToken;

    @Column(name = "fcm_token", length = 512)
    private String fcmToken;

    private Member(String email) {
        this.email = email;
    }

    public static Member from(String email) {
        return new Member(email);
    }

    /**
     * refresh token 변경
     * 로그인 또는 토큰 재발급 시 기존 토큰 값을 새 토큰으로 교체한다.
     */
    public void changeRefreshToken(String token, LocalDateTime expiredAt) {
        this.refreshToken = new RefreshToken(token, expiredAt);
    }

    /**
     * FCM token 변경
     * 클라이언트에서 전달한 최신 디바이스 토큰을 저장한다.
     */
    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
