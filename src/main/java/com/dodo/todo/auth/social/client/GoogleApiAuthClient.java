package com.dodo.todo.auth.social.client;

import com.dodo.todo.auth.social.domain.OAuthUserInfo;
import com.dodo.todo.auth.social.domain.SocialProvider;
import com.dodo.todo.common.exception.ApiException;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class GoogleApiAuthClient implements GoogleAuthClient {

    static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    static final String GOOGLE_USER_INFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;

    public GoogleApiAuthClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret}") String clientSecret
    ) {
        this(restTemplateBuilder.build(), clientId, clientSecret);
    }

    GoogleApiAuthClient(RestTemplate restTemplate, String clientId, String clientSecret) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public OAuthUserInfo authenticate(String authorizationCode, String redirectUri) {
        try {
            GoogleTokenResponse tokenResponse = exchangeAuthorizationCode(authorizationCode, redirectUri);
            validateTokenResponse(tokenResponse);

            GoogleUserInfoResponse userInfoResponse = fetchUserInfo(tokenResponse.accessToken());
            validateUserInfoResponse(userInfoResponse);

            return new OAuthUserInfo(
                    SocialProvider.GOOGLE,
                    userInfoResponse.sub(),
                    userInfoResponse.email(),
                    Boolean.TRUE.equals(userInfoResponse.emailVerified())
            );
        } catch (RestClientException exception) {
            log.warn("Google social authentication failed", exception);
            throw new ApiException(
                    "SOCIAL_AUTHENTICATION_FAILED",
                    HttpStatus.UNAUTHORIZED,
                    "Social authentication failed"
            );
        }
    }

    private GoogleTokenResponse exchangeAuthorizationCode(String authorizationCode, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", authorizationCode);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", AUTHORIZATION_CODE_GRANT_TYPE);

        return restTemplate.postForObject(
                GOOGLE_TOKEN_URL,
                new HttpEntity<>(body, headers),
                GoogleTokenResponse.class
        );
    }

    private GoogleUserInfoResponse fetchUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<GoogleUserInfoResponse> response = restTemplate.exchange(
                GOOGLE_USER_INFO_URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                GoogleUserInfoResponse.class
        );

        return response.getBody();
    }

    private void validateTokenResponse(GoogleTokenResponse tokenResponse) {
        if (tokenResponse == null || tokenResponse.accessToken() == null || tokenResponse.accessToken().isBlank()) {
            throw new ApiException(
                    "SOCIAL_AUTHENTICATION_FAILED",
                    HttpStatus.UNAUTHORIZED,
                    "Social authentication failed"
            );
        }
    }

    private void validateUserInfoResponse(GoogleUserInfoResponse userInfoResponse) {
        if (userInfoResponse == null) {
            throw new ApiException(
                    "SOCIAL_AUTHENTICATION_FAILED",
                    HttpStatus.UNAUTHORIZED,
                    "Social authentication failed"
            );
        }
    }

    record GoogleTokenResponse(
            @JsonProperty("access_token")
            String accessToken
    ) {
    }

    record GoogleUserInfoResponse(
            String sub,
            String email,
            @JsonProperty("email_verified")
            Boolean emailVerified
    ) {
    }
}
