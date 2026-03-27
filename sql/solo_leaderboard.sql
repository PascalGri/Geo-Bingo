-- ============================================================================
-- Solo Leaderboard Table for KatchIt! / GeoBingo
-- Run this in your Supabase SQL Editor
-- ============================================================================

-- Create the solo_scores table
CREATE TABLE IF NOT EXISTS solo_scores (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    player_name TEXT NOT NULL,
    score INTEGER NOT NULL DEFAULT 0,
    categories_count INTEGER NOT NULL DEFAULT 0,
    time_bonus INTEGER NOT NULL DEFAULT 0,
    duration_seconds INTEGER NOT NULL DEFAULT 300,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL
);

-- Index for fast leaderboard queries (top scores)
CREATE INDEX IF NOT EXISTS idx_solo_scores_score_desc
    ON solo_scores (score DESC);

-- Index for player personal best lookups
CREATE INDEX IF NOT EXISTS idx_solo_scores_player_name
    ON solo_scores (player_name, score DESC);

-- Index for recent scores (useful for "latest" views)
CREATE INDEX IF NOT EXISTS idx_solo_scores_created_at
    ON solo_scores (created_at DESC);

-- ============================================================================
-- Row Level Security (RLS)
-- ============================================================================

-- Enable RLS
ALTER TABLE solo_scores ENABLE ROW LEVEL SECURITY;

-- Allow anyone to read all scores (public leaderboard)
CREATE POLICY "Solo scores are publicly readable"
    ON solo_scores
    FOR SELECT
    USING (true);

-- Allow anyone to insert their own score (anonymous inserts via anon key)
CREATE POLICY "Anyone can submit a solo score"
    ON solo_scores
    FOR INSERT
    WITH CHECK (true);

-- Prevent updates and deletes (scores are immutable)
-- No UPDATE or DELETE policies = denied by default with RLS enabled

-- ============================================================================
-- Optional: View for top 100 all-time scores
-- ============================================================================

CREATE OR REPLACE VIEW solo_leaderboard_top100 AS
SELECT
    player_name,
    score,
    categories_count,
    time_bonus,
    duration_seconds,
    created_at
FROM solo_scores
ORDER BY score DESC
LIMIT 100;

-- ============================================================================
-- Optional: View for daily best scores
-- ============================================================================

CREATE OR REPLACE VIEW solo_leaderboard_daily AS
SELECT
    player_name,
    MAX(score) AS best_score,
    COUNT(*) AS games_played,
    MAX(created_at) AS last_played
FROM solo_scores
WHERE created_at >= CURRENT_DATE
GROUP BY player_name
ORDER BY best_score DESC
LIMIT 50;

-- ============================================================================
-- Optional: Function to get player rank
-- ============================================================================

CREATE OR REPLACE FUNCTION get_solo_rank(p_player_name TEXT)
RETURNS TABLE (
    rank BIGINT,
    best_score INTEGER,
    total_games BIGINT
) AS $$
    SELECT
        (SELECT COUNT(*) + 1 FROM (
            SELECT DISTINCT ON (player_name) score
            FROM solo_scores
            ORDER BY player_name, score DESC
        ) top WHERE top.score > (
            SELECT MAX(score) FROM solo_scores WHERE player_name = p_player_name
        )) AS rank,
        (SELECT MAX(score) FROM solo_scores WHERE player_name = p_player_name) AS best_score,
        (SELECT COUNT(*) FROM solo_scores WHERE player_name = p_player_name) AS total_games;
$$ LANGUAGE SQL STABLE;

-- Usage: SELECT * FROM get_solo_rank('Pascal');

-- ============================================================================
-- Optional: Auto-cleanup old low scores (keep top 1000 + last 30 days)
-- Run this periodically via Supabase cron or pg_cron
-- ============================================================================

-- DELETE FROM solo_scores
-- WHERE id NOT IN (
--     SELECT id FROM solo_scores ORDER BY score DESC LIMIT 1000
-- )
-- AND created_at < NOW() - INTERVAL '30 days';
