package com.dodo.todo.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.dodo.todo.common.exception.ApiException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.domain.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("ID로 회원을 조회한다")
    void findById() {
        Member member = Member.of("member@example.com");

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        Member foundMember = memberService.findById(1L);

        assertThat(foundMember).isSameAs(member);
    }

    @Test
    @DisplayName("ID로 회원을 찾지 못하면 예외가 발생한다")
    void rejectMissingMember() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.findById(99L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Member not found");
    }
}
