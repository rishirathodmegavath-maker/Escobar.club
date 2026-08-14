-- Append-only audit trail for campaign schedule (prepone/postpone) changes. Never updated or
-- deleted after insert - one row per successful reschedule, mirroring content_review_notes.
CREATE TABLE campaign_schedule_changes (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id                 BIGINT NOT NULL,
    old_submission_open_at      DATE   NOT NULL,
    old_submission_deadline     DATE   NOT NULL,
    old_publish_start_at        DATE   NOT NULL,
    old_publish_end_at          DATE   NOT NULL,
    new_submission_open_at      DATE   NOT NULL,
    new_submission_deadline     DATE   NOT NULL,
    new_publish_start_at        DATE   NOT NULL,
    new_publish_end_at          DATE   NOT NULL,
    changed_by                  BIGINT NOT NULL,
    changed_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_campaign_schedule_changes_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns (id) ON DELETE CASCADE,
    CONSTRAINT fk_campaign_schedule_changes_changed_by FOREIGN KEY (changed_by) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_campaign_schedule_changes_campaign ON campaign_schedule_changes (campaign_id, changed_at);
