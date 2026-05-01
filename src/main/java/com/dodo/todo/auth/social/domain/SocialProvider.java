package com.dodo.todo.auth.social.domain;

import com.dodo.todo.common.exception.BusinessException;
import java.util.Arrays;
import org.springframework.http.HttpStatus;

public enum SocialProvider {
    GOOGLE;

    public static SocialProvider from(String provider) {
        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "UNSUPPORTED_SOCIAL_PROVIDER",
                        HttpStatus.BAD_REQUEST.value(),
                        "Unsupported social provider"
                ));
    }

    public String getRegistrationId() {
        return name().toLowerCase();
    }
}
