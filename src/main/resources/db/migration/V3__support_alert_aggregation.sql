ALTER TABLE alerts ADD COLUMN dedupe_key VARCHAR(200);
ALTER TABLE alerts ADD CONSTRAINT uk_alerts_dedupe_key UNIQUE (dedupe_key);