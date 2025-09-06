--liquibase formatted sql
--changeset myblog:008-create-audit-trigger-function splitStatements:false endDelimiter:/
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.routines WHERE routine_name = 'audit_posts_trigger_function' AND routine_schema = current_schema()

-- Создание триггерной функции для аудита изменений постов
CREATE OR REPLACE FUNCTION audit_posts_trigger_function()
RETURNS TRIGGER AS '
DECLARE
    change_details_text TEXT := '''';
    user_id_val BIGINT := NULL;
BEGIN
    -- Попытка извлечь user_id из контекста сессии (если установлен)
    -- В реальном приложении это может быть установлено через application_name или custom variable
    BEGIN
        user_id_val := current_setting(''myapp.current_user_id'')::BIGINT;
    EXCEPTION
        WHEN OTHERS THEN
            user_id_val := NULL;
    END;

    -- Обработка INSERT
    IF TG_OP = ''INSERT'' THEN
        change_details_text := ''Создан новый пост'';
        
        INSERT INTO post_audit_log (
            post_id, operation, changed_at, user_id,
            new_title, new_content, new_is_published, change_details
        ) VALUES (
            NEW.id, ''INSERT'', CURRENT_TIMESTAMP, user_id_val,
            NEW.title, NEW.content, NEW.is_published, change_details_text
        );
        
        RETURN NEW;
    END IF;

    -- Обработка UPDATE
    IF TG_OP = ''UPDATE'' THEN
        -- Формируем детальное описание изменений
        change_details_text := ''Изменения: '';
        
        IF OLD.title != NEW.title THEN
            change_details_text := change_details_text || ''заголовок; '';
        END IF;
        
        IF OLD.content != NEW.content THEN
            change_details_text := change_details_text || ''содержимое; '';
        END IF;
        
        IF OLD.is_published != NEW.is_published THEN
            change_details_text := change_details_text || ''статус публикации; '';
        END IF;
        
        -- Удаляем последний "; "
        change_details_text := RTRIM(change_details_text, ''; '');
        
        -- Записываем в лог только если были реальные изменения в отслеживаемых полях
        IF OLD.title != NEW.title OR OLD.content != NEW.content OR OLD.is_published != NEW.is_published THEN
            INSERT INTO post_audit_log (
                post_id, operation, changed_at, user_id,
                old_title, new_title, old_content, new_content,
                old_is_published, new_is_published, change_details
            ) VALUES (
                NEW.id, ''UPDATE'', CURRENT_TIMESTAMP, user_id_val,
                OLD.title, NEW.title, OLD.content, NEW.content,
                OLD.is_published, NEW.is_published, change_details_text
            );
        END IF;
        
        RETURN NEW;
    END IF;

    -- Обработка DELETE
    IF TG_OP = ''DELETE'' THEN
        change_details_text := ''Пост удален'';
        
        INSERT INTO post_audit_log (
            post_id, operation, changed_at, user_id,
            old_title, old_content, old_is_published, change_details
        ) VALUES (
            OLD.id, ''DELETE'', CURRENT_TIMESTAMP, user_id_val,
            OLD.title, OLD.content, OLD.is_published, change_details_text
        );
        
        RETURN OLD;
    END IF;

    RETURN NULL;
END;
' LANGUAGE plpgsql;

--changeset myblog:008-create-audit-trigger
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.triggers WHERE trigger_name = 'audit_posts_trigger' AND event_object_table = 'posts'

-- Создание триггера на таблице posts
DROP TRIGGER IF EXISTS audit_posts_trigger ON posts;

CREATE TRIGGER audit_posts_trigger
    AFTER INSERT OR UPDATE OR DELETE ON posts
    FOR EACH ROW EXECUTE FUNCTION audit_posts_trigger_function();

-- Комментарии
COMMENT ON FUNCTION audit_posts_trigger_function() IS 'Триггерная функция для аудита изменений в таблице posts';

--rollback DROP TRIGGER IF EXISTS audit_posts_trigger ON posts; DROP FUNCTION IF EXISTS audit_posts_trigger_function();