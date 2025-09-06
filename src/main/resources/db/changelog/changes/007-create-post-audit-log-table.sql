--liquibase formatted sql

--changeset myblog:007-create-post-audit-log-table
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.tables WHERE table_name = 'post_audit_log' AND table_schema = current_schema()

CREATE TABLE post_audit_log (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    operation VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT,
    old_title VARCHAR(200),
    new_title VARCHAR(200),
    old_content TEXT,
    new_content TEXT,
    old_is_published BOOLEAN,
    new_is_published BOOLEAN,
    change_details TEXT
);

-- Создание индексов для оптимизации поиска
CREATE INDEX idx_post_audit_log_post_id ON post_audit_log(post_id);
CREATE INDEX idx_post_audit_log_operation ON post_audit_log(operation);
CREATE INDEX idx_post_audit_log_changed_at ON post_audit_log(changed_at);
CREATE INDEX idx_post_audit_log_user_id ON post_audit_log(user_id);

-- Составной индекс для эффективной выборки истории изменений поста
CREATE INDEX idx_post_audit_log_post_changed_at ON post_audit_log(post_id, changed_at DESC);

-- Комментарии к таблице и полям
COMMENT ON TABLE post_audit_log IS 'Журнал аудита изменений постов';
COMMENT ON COLUMN post_audit_log.post_id IS 'ID поста, который был изменен';
COMMENT ON COLUMN post_audit_log.operation IS 'Тип операции: INSERT, UPDATE, DELETE';
COMMENT ON COLUMN post_audit_log.changed_at IS 'Время изменения';
COMMENT ON COLUMN post_audit_log.user_id IS 'ID пользователя, внесшего изменения';
COMMENT ON COLUMN post_audit_log.old_title IS 'Старый заголовок (для UPDATE)';
COMMENT ON COLUMN post_audit_log.new_title IS 'Новый заголовок (для INSERT/UPDATE)';
COMMENT ON COLUMN post_audit_log.old_content IS 'Старое содержимое (для UPDATE)';
COMMENT ON COLUMN post_audit_log.new_content IS 'Новое содержимое (для INSERT/UPDATE)';
COMMENT ON COLUMN post_audit_log.old_is_published IS 'Старый статус публикации (для UPDATE)';
COMMENT ON COLUMN post_audit_log.new_is_published IS 'Новый статус публикации (для INSERT/UPDATE)';
COMMENT ON COLUMN post_audit_log.change_details IS 'Детальное описание изменений';

--rollback DROP TABLE post_audit_log;