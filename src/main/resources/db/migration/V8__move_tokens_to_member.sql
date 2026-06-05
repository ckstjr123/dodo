ALTER TABLE member
    ADD COLUMN refresh_token VARCHAR(512) NULL,
    ADD COLUMN refresh_token_expired_at DATETIME(6) NULL,
    ADD COLUMN fcm_token VARCHAR(512) NULL;

DROP TABLE refresh_token;
