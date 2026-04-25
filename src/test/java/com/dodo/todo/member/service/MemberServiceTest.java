package com.dodo.todo.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.dodo.todo.common.exception.ApiException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.repository.MemberRepository;
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
        Long memberId = 1L;
        String email = "member@example.com";
        Member member = Member.of(email);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        Member foundMember = memberService.findById(memberId);

        assertThat(foundMember).isSameAs(member);
    }

    @Test
    @DisplayName("ID로 회원을 찾을 수 없으면 예외가 발생한다")
    void rejectMissingMember() {
        Long missingMemberId = 99L;
        String expectedMessage = "Member not found";

        when(memberRepository.findById(missingMemberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.findById(missingMemberId))
                .isInstanceOf(ApiException.class)
                .hasMessage(expectedMessage);
    }
}
