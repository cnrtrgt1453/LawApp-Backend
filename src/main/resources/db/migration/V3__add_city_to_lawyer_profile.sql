-- Flyway V3 Migration Script - Add city to lawyer_profiles
ALTER TABLE lawyer_profiles ADD COLUMN IF NOT EXISTS city VARCHAR(100);
