DROP TABLE todo_tag;
DROP TABLE tag;
DROP TABLE todo_repeat;
DROP TABLE reminder;
DROP TABLE checklist;

ALTER TABLE todo
    DROP COLUMN priority,
    ADD COLUMN parent_todo_id BIGINT NULL,
    ADD COLUMN scheduled_date DATE NULL,
    ADD COLUMN scheduled_time TIME NULL,
    ADD COLUMN recurrence JSON NULL;

ALTER TABLE todo
    ADD CONSTRAINT fk_todo_parent
        FOREIGN KEY (parent_todo_id) REFERENCES todo (id);

CREATE TABLE todo_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    todo_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    completed_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_todo_history_member_completed (member_id, completed_at, id),
    KEY idx_todo_history_todo_completed (todo_id, completed_at, id),
    CONSTRAINT fk_todo_history_member
        FOREIGN KEY (member_id) REFERENCES member (id)
);
