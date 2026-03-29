-- Add user_id column to players table (nullable, for guest play support)
ALTER TABLE players ADD COLUMN IF NOT EXISTS user_id TEXT DEFAULT NULL;

-- Add user_id column to solo_scores table (nullable, for guest play support)
ALTER TABLE solo_scores ADD COLUMN IF NOT EXISTS user_id TEXT DEFAULT NULL;

-- Index for efficient lookups by user_id
CREATE INDEX IF NOT EXISTS idx_players_user_id ON players(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_solo_scores_user_id ON solo_scores(user_id) WHERE user_id IS NOT NULL;
