-- Add missing fields to existing tables for real-time progress tracking and user feedback
-- Migration: V2__add_progress_and_feedback_fields.sql

-- Add progress tracking to analyses table
ALTER TABLE analyses
ADD COLUMN IF NOT EXISTS progress INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS progress_message VARCHAR(500);

-- Update existing rows to have progress = 100 if completed
UPDATE analyses
SET progress = 100
WHERE status = 'completed' AND progress IS NULL;

-- Add feedback and response tracking to suggestions table
ALTER TABLE suggestions
ADD COLUMN IF NOT EXISTS responded_at TIMESTAMP;

-- Create index on responded_at for analytics queries
CREATE INDEX IF NOT EXISTS idx_suggestions_responded_at
ON suggestions(responded_at)
WHERE responded_at IS NOT NULL;

-- Create index on analysis progress for filtering in-progress analyses
CREATE INDEX IF NOT EXISTS idx_analyses_progress
ON analyses(progress)
WHERE status = 'processing';

-- Add comments for documentation
COMMENT ON COLUMN analyses.progress IS 'Analysis progress percentage (0-100) for real-time UI updates';
COMMENT ON COLUMN analyses.progress_message IS 'Current progress message shown to user';
COMMENT ON COLUMN suggestions.responded_at IS 'Timestamp when user accepted/rejected suggestion';
