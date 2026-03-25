-- ============================================================
-- RLS FIX: category_swaps, eliminations, sabotages + captures cleanup
-- Run this in Supabase SQL Editor (Dashboard → SQL Editor)
-- ============================================================

-- category_swaps: RLS einschalten
ALTER TABLE category_swaps ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow anon full access on category_swaps" ON category_swaps;
CREATE POLICY "Allow anon full access on category_swaps"
    ON category_swaps
    FOR ALL
    TO anon, authenticated
    USING (true)
    WITH CHECK (true);

-- eliminations: RLS einschalten
ALTER TABLE eliminations ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow anon full access on eliminations" ON eliminations;
CREATE POLICY "Allow anon full access on eliminations"
    ON eliminations
    FOR ALL
    TO anon, authenticated
    USING (true)
    WITH CHECK (true);

-- sabotages: RLS einschalten
ALTER TABLE sabotages ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow anon full access on sabotages" ON sabotages;
CREATE POLICY "Allow anon full access on sabotages"
    ON sabotages
    FOR ALL
    TO anon, authenticated
    USING (true)
    WITH CHECK (true);

-- captures: alle alten Policies loeschen, eine saubere neu anlegen
DO $$
DECLARE
    pol RECORD;
BEGIN
    FOR pol IN
        SELECT policyname FROM pg_policies
        WHERE schemaname = 'public' AND tablename = 'captures'
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON captures', pol.policyname);
        RAISE NOTICE 'Dropped policy: %', pol.policyname;
    END LOOP;
END $$;

CREATE POLICY "Allow anon full access on captures"
    ON captures
    FOR ALL
    TO anon, authenticated
    USING (true)
    WITH CHECK (true);

-- ========================
-- VERIFY
-- ========================
SELECT tablename, policyname, cmd, roles
FROM pg_policies
WHERE schemaname = 'public'
  AND tablename IN ('category_swaps', 'eliminations', 'sabotages', 'captures')
ORDER BY tablename, policyname;
