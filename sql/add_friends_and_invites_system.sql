-- Add last_seen for online presence tracking
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS last_seen TIMESTAMPTZ DEFAULT now();
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS friend_code TEXT DEFAULT NULL;

-- Unique friend codes for adding friends
CREATE UNIQUE INDEX IF NOT EXISTS idx_profiles_friend_code ON profiles(friend_code) WHERE friend_code IS NOT NULL;

-- Friendships table (bidirectional, stored once with user_id < friend_id)
CREATE TABLE IF NOT EXISTS friendships (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id TEXT NOT NULL,
    friend_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',
    requested_by TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    UNIQUE(user_id, friend_id)
);

CREATE INDEX IF NOT EXISTS idx_friendships_user ON friendships(user_id);
CREATE INDEX IF NOT EXISTS idx_friendships_friend ON friendships(friend_id);

-- Game invites table
CREATE TABLE IF NOT EXISTS game_invites (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    from_user_id TEXT NOT NULL,
    to_user_id TEXT NOT NULL,
    game_code TEXT NOT NULL,
    game_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_game_invites_to ON game_invites(to_user_id, status);

-- Enable realtime for game_invites so invites are pushed instantly
ALTER PUBLICATION supabase_realtime ADD TABLE game_invites;
ALTER PUBLICATION supabase_realtime ADD TABLE friendships;
