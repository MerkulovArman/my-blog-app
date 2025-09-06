--liquibase formatted sql

--changeset myblog:006-create-likes-table
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.tables WHERE table_name = 'likes' AND table_schema = current_schema()

CREATE TABLE likes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_likes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_likes_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT uk_likes_user_post UNIQUE (user_id, post_id)
);

-- Создание индексов для оптимизации поиска
CREATE INDEX idx_likes_user_id ON likes(user_id);
CREATE INDEX idx_likes_post_id ON likes(post_id);
CREATE INDEX idx_likes_is_active ON likes(is_active);

-- Составной индекс для подсчета активных лайков поста
CREATE INDEX idx_likes_post_active ON likes(post_id, is_active);

-- Комментарии к таблице и полям
COMMENT ON TABLE likes IS 'Таблица лайков постов';
COMMENT ON COLUMN likes.user_id IS 'ID пользователя, поставившего лайк';
COMMENT ON COLUMN likes.post_id IS 'ID поста, который лайкнули';
COMMENT ON COLUMN likes.is_active IS 'Активен ли лайк';

--rollback DROP TABLE likes;