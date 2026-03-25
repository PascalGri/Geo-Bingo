-- ============================================================
-- COMPLETE FIX: Constraints + RLS for vote_submissions & votes
-- Run this in Supabase SQL Editor (Dashboard → SQL Editor)
-- ============================================================

-- =====================
-- PART 1: RLS POLICIES
-- =====================
-- Ensure anon users can INSERT and SELECT on both tables.
-- Without this, players can't see each other's votes.

-- vote_submissions: allow full access for anon
ALTER TABLE vote_submissions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow anon full access on vote_submissions" ON vote_submissions;
CREATE POLICY "Allow anon full access on vote_submissions"
    ON vote_submissions
    FOR ALL
    USING (true)
    WITH CHECK (true);

-- votes: allow full access for anon
ALTER TABLE votes ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow anon full access on votes" ON votes;
CREATE POLICY "Allow anon full access on votes"
    ON votes
    FOR ALL
    USING (true)
    WITH CHECK (true);

-- captures: ensure all players can see each other's captures
ALTER TABLE captures ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow anon full access on captures" ON captures;
CREATE POLICY "Allow anon full access on captures"
    ON captures
    FOR ALL
    USING (true)
    WITH CHECK (true);

-- games: ensure all players can read/update game status
ALTER TABLE games ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow anon full access on games" ON games;
CREATE POLICY "Allow anon full access on games"
    ON games
    FOR ALL
    USING (true)
    WITH CHECK (true);

-- players: ensure all players can see each other
ALTER TABLE players ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow anon full access on players" ON players;
CREATE POLICY "Allow anon full access on players"
    ON players
    FOR ALL
    USING (true)
    WITH CHECK (true);

-- categories
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow anon full access on categories" ON categories;
CREATE POLICY "Allow anon full access on categories"
    ON categories
    FOR ALL
    USING (true)
    WITH CHECK (true);

-- ========================
-- PART 2: FIX CONSTRAINTS
-- ========================

-- Fix vote_submissions: drop all existing unique constraints, add correct one
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'vote_submissions'::regclass AND contype = 'u'
    LOOP
        EXECUTE 'ALTER TABLE vote_submissions DROP CONSTRAINT ' || quote_ident(r.conname);
        RAISE NOTICE 'Dropped: %', r.conname;
    END LOOP;
END $$;

ALTER TABLE vote_submissions
    ADD CONSTRAINT vote_submissions_game_voter_category_unique
    UNIQUE (game_id, voter_id, category_id);

-- Fix votes: clean duplicates first, then add constraint
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'votes'::regclass AND contype = 'u'
    LOOP
        EXECUTE 'ALTER TABLE votes DROP CONSTRAINT ' || quote_ident(r.conname);
        RAISE NOTICE 'Dropped: %', r.conname;
    END LOOP;
END $$;

DELETE FROM votes
WHERE id NOT IN (
    SELECT DISTINCT ON (game_id, voter_id, target_player_id, category_id) id
    FROM votes
    ORDER BY game_id, voter_id, target_player_id, category_id, id DESC
);

ALTER TABLE votes
    ADD CONSTRAINT votes_game_voter_target_category_unique
    UNIQUE (game_id, voter_id, target_player_id, category_id);

-- ========================
-- PART 3: VERIFY
-- ========================
SELECT 'CONSTRAINTS' as check_type, conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid IN ('vote_submissions'::regclass, 'votes'::regclass)
  AND contype = 'u';
