-- Flyway V2 Migration Script - Add youtube_url and drop intro_video_url
ALTER TABLE lawyer_profiles DROP COLUMN IF EXISTS intro_video_url;
ALTER TABLE lawyer_profiles ADD COLUMN IF NOT EXISTS youtube_url VARCHAR(255);
