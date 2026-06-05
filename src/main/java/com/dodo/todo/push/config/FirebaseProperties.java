package com.dodo.todo.push.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "firebase")
public class FirebaseProperties {

    private String serviceAccountPath = "classpath:firebase-service-account.json";

    public String getServiceAccountPath() {
        return serviceAccountPath;
    }

    public void setServiceAccountPath(String serviceAccountPath) {
        this.serviceAccountPath = serviceAccountPath;
    }
}
