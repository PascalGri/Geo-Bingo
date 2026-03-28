-- Add avatar_url column to profiles table for persistent profile pictures
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS avatar_url TEXT DEFAULT '';
