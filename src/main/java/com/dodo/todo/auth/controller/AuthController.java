package com.dodo.todo.auth.controller;

import com.dodo.todo.auth.dto.LoginRequest;
import com.dodo.todo.auth.dto.LoginResponse;
import com.dodo.todo.auth.dto.MemberResponse;
import com.dodo.todo.auth.dto.RefreshTokenRequest;
import com.dodo.todo.auth.dto.SignupRequest;
import com.dodo.todo.auth.dto.TokenResponse;
import com.dodo.todo.auth.resolver.LoginMember;
import com.dodo.todo.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApiDocs {

    private final AuthService authService;

    @Override
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @Override
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Override
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @Override
    @GetMapping("/me")
    public MemberResponse me(@LoginMember Long memberId) {
        return authService.getCurrentMember(memberId);
    }
}
