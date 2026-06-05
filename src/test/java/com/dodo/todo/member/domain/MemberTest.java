package com.dodo.todo.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    @DisplayName("refresh token을 변경한다")
    void changeRefreshToken() {
        Member member = Member.from("member@example.com");
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(1);

        member.changeRefreshToken("refresh-token", expiredAt);

        assertThat(member.getRefreshToken().getToken()).isEqualTo("refresh-token");
        assertThat(member.getRefreshToken().getExpiredAt()).isEqualTo(expiredAt);
    }

    @Test
    @DisplayName("저장된 refresh token이 일치하고 만료되지 않으면 유효하다")
    void validRefreshToken() {
        Member member = Member.from("member@example.com");
        member.changeRefreshToken("refresh-token", LocalDateTime.now().plusDays(1));

        assertThat(member.getRefreshToken().isValidRefreshToken("refresh-token")).isTrue();
    }

    @Test
    @DisplayName("만료된 refresh token은 유효하지 않다")
    void expiredRefreshToken() {
        Member member = Member.from("member@example.com");
        member.changeRefreshToken("refresh-token", LocalDateTime.now().minusMinutes(1));

        assertThat(member.getRefreshToken().isExpired()).isTrue();
        assertThat(member.getRefreshToken().isValidRefreshToken("refresh-token")).isFalse();
    }

    @Test
    @DisplayName("FCM token을 변경한다")
    void updateFcmToken() {
        Member member = Member.from("member@example.com");

        member.updateFcmToken("fcm-token");

        assertThat(member.getFcmToken()).isEqualTo("fcm-token");
    }
}
