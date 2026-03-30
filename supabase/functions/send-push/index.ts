import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

// FCM V1 API: Get OAuth2 access token from service account
async function getAccessToken(): Promise<string> {
  const serviceAccountJson = Deno.env.get("FCM_SERVICE_ACCOUNT");
  if (!serviceAccountJson) throw new Error("FCM_SERVICE_ACCOUNT not set");

  const sa = JSON.parse(serviceAccountJson);
  const now = Math.floor(Date.now() / 1000);

  // Create JWT header and claim set
  const header = { alg: "RS256", typ: "JWT" };
  const claimSet = {
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  };

  // Base64url encode
  const b64url = (obj: unknown) =>
    btoa(JSON.stringify(obj))
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
      .replace(/=+$/, "");
  const unsignedToken = `${b64url(header)}.${b64url(claimSet)}`;

  // Sign with RSA private key
  const pemContent = sa.private_key
    .replace(/-----BEGIN PRIVATE KEY-----/g, "")
    .replace(/-----END PRIVATE KEY-----/g, "")
    .replace(/\n/g, "");
  const binaryKey = Uint8Array.from(atob(pemContent), (c: string) =>
    c.charCodeAt(0)
  );

  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    binaryKey,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );

  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    new TextEncoder().encode(unsignedToken)
  );

  const sig = btoa(String.fromCharCode(...new Uint8Array(signature)))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
  const jwt = `${unsignedToken}.${sig}`;

  // Exchange JWT for access token
  const tokenRes = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
  });

  const tokenData = await tokenRes.json();
  if (!tokenData.access_token)
    throw new Error(`Token exchange failed: ${JSON.stringify(tokenData)}`);
  return tokenData.access_token;
}

Deno.serve(async (req: Request) => {
  try {
    const { to_user_id, title, body, data } = await req.json();
    if (!to_user_id || !title || !body) {
      return new Response(JSON.stringify({ error: "Missing fields" }), {
        status: 400,
      });
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const supabase = createClient(supabaseUrl, serviceKey);

    // Get device tokens for user
    const { data: tokens, error } = await supabase
      .from("device_tokens")
      .select("token, platform")
      .eq("user_id", to_user_id);

    if (error || !tokens || tokens.length === 0) {
      return new Response(JSON.stringify({ sent: 0 }), { status: 200 });
    }

    // Get FCM access token and project ID
    const accessToken = await getAccessToken();
    const sa = JSON.parse(Deno.env.get("FCM_SERVICE_ACCOUNT")!);
    const projectId = sa.project_id;

    let sent = 0;

    for (const { token } of tokens) {
      try {
        const res = await fetch(
          `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${accessToken}`,
            },
            body: JSON.stringify({
              message: {
                token,
                notification: { title, body },
                data: data || {},
              },
            }),
          }
        );
        if (res.ok) sent++;
        else console.error("FCM send failed:", await res.text());
      } catch (e) {
        console.error("FCM send error:", e);
      }
    }

    return new Response(JSON.stringify({ sent }), {
      headers: { "Content-Type": "application/json" },
    });
  } catch (e) {
    console.error("Push function error:", e);
    return new Response(JSON.stringify({ error: (e as Error).message }), {
      status: 500,
    });
  }
});
