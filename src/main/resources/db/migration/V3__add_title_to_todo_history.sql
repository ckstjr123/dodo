ALTER TABLE todo_history
    ADD COLUMN title VARCHAR(200) NOT NULL AFTER member_id;
