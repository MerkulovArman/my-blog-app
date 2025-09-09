--liquibase formatted sql

--changeset myblog:013-create-materialized-view-log-table
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.tables WHERE table_name = 'materialized_view_refresh_log' AND table_schema = current_schema()

-- Создаем таблицу для логирования обновлений материализованного представления
CREATE TABLE materialized_view_refresh_log (
    id BIGSERIAL PRIMARY KEY,
    view_name VARCHAR(255) NOT NULL,
    refresh_type VARCHAR(50) NOT NULL, -- TRIGGER, SCHEDULED, MANUAL
    triggered_by_table VARCHAR(255),
    triggered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    duration_ms BIGINT,
    success BOOLEAN DEFAULT true,
    error_message TEXT
);

-- Создаем индексы для логов
CREATE INDEX idx_mv_refresh_log_view_name ON materialized_view_refresh_log(view_name);
CREATE INDEX idx_mv_refresh_log_triggered_at ON materialized_view_refresh_log(triggered_at);

-- Комментарии
COMMENT ON TABLE materialized_view_refresh_log IS 'Лог обновлений материализованных представлений';
COMMENT ON COLUMN materialized_view_refresh_log.refresh_type IS 'Тип обновления: TRIGGER (автоматически), SCHEDULED (по расписанию), MANUAL (вручную)';

--rollback DROP TABLE IF EXISTS materialized_view_refresh_log;