ALTER TABLE creator_profiles
    ADD COLUMN open_to_other_niches BOOLEAN NOT NULL DEFAULT FALSE AFTER niche;
