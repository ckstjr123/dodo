package com.dodo.todo.member.service;

import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.member.dto.FcmTokenRequest;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    /**
     * 회원 조회
     * ID로 회원을 조회하고 없으면 404 예외를 발생시킴.
     */
    @Transactional(readOnly = true)
    public Member findById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        "MEMBER_NOT_FOUND",
                        HttpStatus.NOT_FOUND.value(),
                        "Member not found"
                ));
    }

    /**
     * FCM token 변경
     * 로그인한 회원의 디바이스 토큰을 최신 값으로 교체한다.
     */
    @Transactional
    public void updateFcmToken(Long memberId, FcmTokenRequest request) {
        Member member = findById(memberId);

        member.updateFcmToken(request.fcmToken());
    }
}
