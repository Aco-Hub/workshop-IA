CREATE TABLE whiteboard_rooms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_by BIGINT NOT NULL REFERENCES developers(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE whiteboard_elements (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES whiteboard_rooms(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL,
    data JSONB NOT NULL,
    color VARCHAR(30) NOT NULL DEFAULT '#000000',
    stroke_width INTEGER NOT NULL DEFAULT 2,
    created_by BIGINT NOT NULL REFERENCES developers(id),
    z_index INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE whiteboard_comments (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES whiteboard_rooms(id) ON DELETE CASCADE,
    developer_id BIGINT NOT NULL REFERENCES developers(id),
    text TEXT NOT NULL,
    parent_id BIGINT REFERENCES whiteboard_comments(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wb_elements_room ON whiteboard_elements(room_id);
CREATE INDEX idx_wb_comments_room ON whiteboard_comments(room_id);
CREATE INDEX idx_wb_rooms_created_by ON whiteboard_rooms(created_by);
