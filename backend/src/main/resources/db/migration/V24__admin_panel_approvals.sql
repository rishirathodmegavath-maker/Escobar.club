ALTER TABLE business_profiles ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
-- Grandfather businesses that already existed before this feature shipped; only new registrations
-- start PENDING and need an admin decision.
UPDATE business_profiles SET approval_status = 'APPROVED';

ALTER TABLE campaigns ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE campaigns ADD COLUMN admin_display_status VARCHAR(20) NULL;
-- Grandfather campaigns that were already published and visible before this feature shipped.
UPDATE campaigns SET approval_status = 'APPROVED' WHERE status = 'PUBLISHED';
