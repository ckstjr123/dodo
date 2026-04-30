package com.dodo.todo.auth.social.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import com.dodo.todo.auth.social.domain.SocialProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

class GoogleApiAuthClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("authorization code를 Google에 검증하고 사용자 정보를 반환한다")
    void authenticateReturnsOAuthUserInfo() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        GoogleApiAuthClient client = new GoogleApiAuthClient(restTemplate, "client-id", "client-secret");

        server.expect(once(), requestTo(GoogleApiAuthClient.GOOGLE_TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess(toJson(tokenResponse()), MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo(GoogleApiAuthClient.GOOGLE_USER_INFO_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer google-access-token"))
                .andRespond(withSuccess(toJson(userInfoResponse()), MediaType.APPLICATION_JSON));

        var userInfo = client.authenticate("google-code", "http://localhost:5173/auth/callback");

        assertThat(userInfo.provider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(userInfo.providerUserId()).isEqualTo("google-123");
        assertThat(userInfo.email()).isEqualTo("google@example.com");
        assertThat(userInfo.emailVerified()).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("Google token 교환이 실패하면 RestTemplate 예외를 전파한다")
    void authenticateThrowsRestClientExceptionWhenGoogleCallFails() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        GoogleApiAuthClient client = new GoogleApiAuthClient(restTemplate, "client-id", "client-secret");

        server.expect(once(), requestTo(GoogleApiAuthClient.GOOGLE_TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> client.authenticate("bad-code", "http://localhost:5173/auth/callback"))
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
        server.verify();
    }

    private GoogleApiAuthClient.GoogleTokenResponse tokenResponse() {
        return new GoogleApiAuthClient.GoogleTokenResponse("google-access-token");
    }

    private GoogleApiAuthClient.GoogleUserInfoResponse userInfoResponse() {
        return new GoogleApiAuthClient.GoogleUserInfoResponse(
                "google-123",
                "google@example.com",
                true
        );
    }

    private String toJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }
}
