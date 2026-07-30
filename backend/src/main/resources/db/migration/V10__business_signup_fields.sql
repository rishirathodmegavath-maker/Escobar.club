ALTER TABLE business_profiles
    ADD COLUMN gst_number VARCHAR(20) NOT NULL DEFAULT '' AFTER company_name,
    ADD COLUMN contact_person_name VARCHAR(150) NOT NULL DEFAULT '' AFTER gst_number,
    ADD COLUMN mobile_number VARCHAR(20) NOT NULL DEFAULT '' AFTER contact_person_name;
