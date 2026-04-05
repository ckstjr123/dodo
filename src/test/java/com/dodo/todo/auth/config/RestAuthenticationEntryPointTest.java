package com.dodo.todo.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

class RestAuthenticationEntryPointTest {

    private final RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(new ObjectMapper());

    @Test
    @DisplayName("인증 실패 시 401 상태와 UNAUTHORIZED 코드를 반환한다")
    void returnsUnauthorizedStatusAndCode() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new AuthenticationException("Unauthorized") {});

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        String body = response.getContentAsString();
        assertThat(body).contains("UNAUTHORIZED");
        assertThat(body).contains("Authentication is required");
    }

    @Test
    @DisplayName("인증 실패 응답은 UTF-8 인코딩을 사용한다")
    void usesUtf8Encoding() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new AuthenticationException("unauthorized") {});

        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
    }
}