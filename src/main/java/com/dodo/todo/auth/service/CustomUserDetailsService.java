package com.dodo.todo.auth.service;

import com.dodo.todo.auth.principal.MemberPrincipal;
import com.dodo.todo.common.exception.ApiException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public MemberPrincipal loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Member not found"));

        return toPrincipal(member);
    }

    public MemberPrincipal loadUserById(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException("MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND, "Member not found"));

        return toPrincipal(member);
    }

    private MemberPrincipal toPrincipal(Member member) {
        return new MemberPrincipal(
                member.getId(),
                member.getEmail(),
                member.getPassword(),
                member.getNickname()
        );
    }
}
