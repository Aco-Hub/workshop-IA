ALTER TABLE time_entries ADD COLUMN recurrence_group_id UUID;
ALTER TABLE time_entries ADD COLUMN recurrence_rule VARCHAR(255);
CREATE INDEX idx_time_entries_recurrence_group ON time_entries(recurrence_group_id);
