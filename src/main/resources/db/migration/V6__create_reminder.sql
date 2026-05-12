CREATE TABLE reminder (
    id BIGINT NOT NULL AUTO_INCREMENT,
    todo_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    remind_at DATETIME(6) NOT NULL,
    minute_offset INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_reminder_todo
        FOREIGN KEY (todo_id) REFERENCES todo (id),
    CONSTRAINT fk_reminder_member
        FOREIGN KEY (member_id) REFERENCES member (id)
);
