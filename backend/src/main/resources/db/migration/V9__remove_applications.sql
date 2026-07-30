-- Removes the "apply to a campaign, wait for approval" step entirely. Creators now submit
-- content directly against an open campaign, with no limit on how many pieces of content
-- they can submit to the same campaign. Business review still happens, just at the content
-- stage instead of a separate application stage.
ALTER TABLE content
    DROP FOREIGN KEY fk_content_application,
    DROP COLUMN application_id;

DROP TABLE applications;
