-- Historical rows created before GST number was required may hold the empty-string
-- placeholder default from V10. Null them out first since MySQL allows multiple NULLs
-- under a UNIQUE constraint (unlike multiple empty strings, which would collide).
UPDATE business_profiles SET gst_number = NULL WHERE gst_number = '';

ALTER TABLE business_profiles
    MODIFY COLUMN gst_number VARCHAR(20) NULL;

ALTER TABLE business_profiles
    ADD CONSTRAINT uk_business_profiles_gst_number UNIQUE (gst_number);
