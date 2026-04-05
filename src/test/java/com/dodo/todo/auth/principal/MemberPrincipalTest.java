package com.dodo.todo.auth.principal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberPrincipalTest {

    @Test
    @DisplayName("getUsername은 이메일을 반환한다")
    void getUsernameReturnsEmail() {
        MemberPrincipal principal = new MemberPrincipal(1L, "user@example.com", "encoded", "user");

        assertThat(principal.getUsername()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("getAuthorities는 빈 목록을 반환한다")
    void getAuthoritiesReturnsEmptyList() {
        MemberPrincipal principal = new MemberPrincipal(1L, "user@example.com", "encoded", "user");

        assertThat(principal.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("모든 필드를 생성자에서 올바르게 저장한다")
    void storesAllFieldsFromConstructor() {
        MemberPrincipal principal = new MemberPrincipal(42L, "a@b.com", "hashed", "nickname");

        assertThat(principal.getId()).isEqualTo(42L);
        assertThat(principal.getEmail()).isEqualTo("a@b.com");
        assertThat(principal.getPassword()).isEqualTo("hashed");
        assertThat(principal.getNickname()).isEqualTo("nickname");
    }
}