// Edge Function: send-daily-reminders
// Intended to be called via a cron job (e.g., Supabase pg_cron or external scheduler)
// Sends push notifications to users who haven't played today to remind them of:
// - Daily Challenge
// - Daily Login Bonus
// - Weekly Challenge progress
// - Solo mode personal best opportunity

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const fcmServiceAccount = Deno.env.get("FCM_SERVICE_ACCOUNT");

interface DeviceToken {
  user_id: string;
  token: string;
  platform: string;
}

async function getAccessToken(): Promise<string> {
  if (!fcmServiceAccount) throw new Error("FCM_SERVICE_ACCOUNT not set");
  const sa = JSON.parse(fcmServiceAccount);
  const now = Math.floor(Date.now() / 1000);
  const header = btoa(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const payload = btoa(
    JSON.stringify({
      iss: sa.client_email,
      scope: "https://www.googleapis.com/auth/firebase.messaging",
      aud: "https://oauth2.googleapis.com/token",
      iat: now,
      exp: now + 3600,
    })
  );

  const encoder = new TextEncoder();
  const data = encoder.encode(`${header}.${payload}`);
  const keyData = sa.private_key
    .replace(/-----BEGIN PRIVATE KEY-----/g, "")
    .replace(/-----END PRIVATE KEY-----/g, "")
    .replace(/\n/g, "");
  const binaryKey = Uint8Array.from(atob(keyData), (c) => c.charCodeAt(0));
  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    binaryKey,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", cryptoKey, data);
  const jwt = `${header}.${payload}.${btoa(String.fromCharCode(...new Uint8Array(signature)))}`;

  const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
  });
  const tokenData = await tokenResponse.json();
  return tokenData.access_token;
}

async function sendFcmNotification(
  accessToken: string,
  projectId: string,
  deviceToken: string,
  title: string,
  body: string,
  data: Record<string, string> = {}
): Promise<boolean> {
  try {
    const response = await fetch(
      `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token: deviceToken,
            notification: { title, body },
            data,
          },
        }),
      }
    );
    return response.ok;
  } catch {
    return false;
  }
}

// Notification templates (German + fallback)
const NOTIFICATIONS = [
  {
    id: "daily_challenge",
    title: "Taegliche Challenge wartet!",
    body: "Schliesse deine taegliche Challenge ab und verdiene Stars!",
  },
  {
    id: "login_bonus",
    title: "Dein taeglicher Bonus wartet!",
    body: "Oeffne KatchIt! und sichere dir deine taeglichen Stars.",
  },
  {
    id: "solo_challenge",
    title: "Bereit fuer eine Solo Challenge?",
    body: "Verbessere deinen Highscore im Solo-Modus!",
  },
  {
    id: "weekly_challenge",
    title: "Woechentliche Challenge",
    body: "Vergiss nicht deine woechentliche Challenge abzuschliessen!",
  },
  {
    id: "play_reminder",
    title: "Deine Freunde spielen!",
    body: "Starte eine Runde KatchIt! und fordere sie heraus.",
  },
];

serve(async (_req) => {
  try {
    if (!fcmServiceAccount) {
      return new Response(JSON.stringify({ error: "FCM not configured" }), { status: 500 });
    }

    const sa = JSON.parse(fcmServiceAccount);
    const projectId = sa.project_id;
    const accessToken = await getAccessToken();

    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    // Get all device tokens
    const { data: tokens, error } = await supabase
      .from("device_tokens")
      .select("user_id, token, platform");

    if (error || !tokens || tokens.length === 0) {
      return new Response(JSON.stringify({ sent: 0, reason: "no tokens" }), { status: 200 });
    }

    // Pick a random notification for variety
    const notification = NOTIFICATIONS[Math.floor(Math.random() * NOTIFICATIONS.length)];
    let sent = 0;

    // Send to all unique users (one token per user to avoid duplicates)
    const seenUsers = new Set<string>();
    for (const t of tokens as DeviceToken[]) {
      if (seenUsers.has(t.user_id)) continue;
      seenUsers.add(t.user_id);

      const ok = await sendFcmNotification(
        accessToken,
        projectId,
        t.token,
        notification.title,
        notification.body,
        { type: notification.id }
      );
      if (ok) sent++;
    }

    return new Response(
      JSON.stringify({ sent, total: seenUsers.size, notification: notification.id }),
      { status: 200, headers: { "Content-Type": "application/json" } }
    );
  } catch (e) {
    return new Response(JSON.stringify({ error: String(e) }), { status: 500 });
  }
});
