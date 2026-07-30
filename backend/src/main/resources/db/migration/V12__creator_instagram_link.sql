ALTER TABLE creator_profiles
    ADD COLUMN instagram_profile_url VARCHAR(500) NOT NULL DEFAULT '' AFTER niche;

DROP TABLE creator_social_links;
