create TABLE IF NOT EXISTS interactions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT uk_user_event UNIQUE (user_id, event_id)
);

create index IF NOT EXISTS idx_interactions_user_id ON interactions(user_id);
create index IF NOT EXISTS idx_interactions_event_id ON interactions(event_id);

create TABLE IF NOT EXISTS similarities (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_a BIGINT NOT NULL,
    event_b BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT uk_events_pair UNIQUE (event_a, event_b),
    CONSTRAINT chk_events_order CHECK (event_a < event_b)
);

create index IF NOT EXISTS idx_similarities_event_a ON similarities(event_a);
create index IF NOT EXISTS idx_similarities_event_b ON similarities(event_b);
create index IF NOT EXISTS idx_similarities_score ON similarities(score DESC);