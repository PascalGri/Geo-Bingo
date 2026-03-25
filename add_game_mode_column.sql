-- ============================================================
-- MIGRATION: game_mode Spalte zur games-Tabelle hinzufügen
-- Run this in Supabase SQL Editor (Dashboard → SQL Editor)
-- ============================================================

ALTER TABLE games
    ADD COLUMN IF NOT EXISTS game_mode TEXT NOT NULL DEFAULT 'CLASSIC';

-- VERIFY
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'games'
  AND column_name = 'game_mode';
