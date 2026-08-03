-- Rename the old visibility window to what it now means: the publishing window.
ALTER TABLE campaigns CHANGE COLUMN start_date publish_start_at DATE NOT NULL;
ALTER TABLE campaigns CHANGE COLUMN end_date publish_end_at DATE NOT NULL;

-- New creator-submission window, nullable until backfilled below.
ALTER TABLE campaigns ADD COLUMN submission_open_at DATE NULL AFTER description;
ALTER TABLE campaigns ADD COLUMN submission_deadline DATE NULL AFTER submission_open_at;

-- Backfill existing rows: submissions opened when the campaign was created and closed when
-- the publishing window was due to start.
UPDATE campaigns SET submission_open_at = DATE(created_at) WHERE submission_open_at IS NULL;
UPDATE campaigns SET submission_deadline = publish_start_at WHERE submission_deadline IS NULL;
-- Guard against inversion for rows whose publish_start_at was already before their created_at.
UPDATE campaigns SET submission_open_at = submission_deadline WHERE submission_open_at > submission_deadline;

ALTER TABLE campaigns MODIFY COLUMN submission_open_at DATE NOT NULL;
ALTER TABLE campaigns MODIFY COLUMN submission_deadline DATE NOT NULL;

-- Old STARTING_SOON/ACTIVE/CLOSED values are now computed automatically from dates instead of stored;
-- collapse them all to PUBLISHED so the lifecycle is derived going forward. DRAFT rows are untouched.
UPDATE campaigns SET status = 'PUBLISHED' WHERE status IN ('STARTING_SOON', 'ACTIVE', 'CLOSED');

DROP INDEX idx_campaigns_dates ON campaigns;
CREATE INDEX idx_campaigns_dates ON campaigns (publish_start_at, publish_end_at);
CREATE INDEX idx_campaigns_submission_dates ON campaigns (submission_open_at, submission_deadline);
