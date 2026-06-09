package com.dodo.todo.member.controller;

import com.dodo.todo.auth.resolver.LoginMember;
import com.dodo.todo.member.dto.FcmTokenRequest;
import com.dodo.todo.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * FCM token 등록
     * 현재 로그인한 회원의 최신 디바이스 토큰을 저장한다.
     */
    @PatchMapping("/me/fcm-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateFcmToken(@LoginMember Long memberId, @Valid @RequestBody FcmTokenRequest request) {
        memberService.updateFcmToken(memberId, request);
    }
}
