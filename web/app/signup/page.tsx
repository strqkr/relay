"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/apiClient";
import { setStoredSession } from "@/lib/session";
import { useIsConnected } from "@/lib/useConnection";
import type { AuthResponse } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

export default function SignupPage() {
  const router = useRouter();
  const connected = useIsConnected();
  const [organizationName, setOrganizationName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [issuedKey, setIssuedKey] = useState<AuthResponse | null>(null);

  // Only bounce an already-connected visitor away before they've signed up here — not right
  // after signup itself, which is what makes `connected` flip true while we still need to
  // show them their one-time API key below.
  useEffect(() => {
    if (connected && !issuedKey) {
      router.push("/endpoints");
    }
  }, [connected, issuedKey, router]);

  if (connected && !issuedKey) {
    return null;
  }

  async function signup(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const auth = await api.post<AuthResponse>("/auth/signup", { organizationName, email, password });
      setStoredSession({ organizationId: auth.organizationId, organizationName: auth.organizationName, email: auth.email });
      setIssuedKey(auth);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong.");
    } finally {
      setBusy(false);
    }
  }

  if (issuedKey) {
    return (
      <div className="mx-auto flex max-w-md flex-col gap-6 pt-16">
        <Card>
          <CardHeader>
            <CardTitle>Welcome to relay, {issuedKey.organizationName}</CardTitle>
            <CardDescription>Save your API key now — you won&apos;t be able to see it again.</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <Input readOnly className="font-mono text-sm" value={issuedKey.apiKey ?? ""} onFocus={(e) => e.target.select()} />
            <Alert>
              <AlertTitle>Use this key from your own systems</AlertTitle>
              <AlertDescription>
                You&apos;re already signed in to this dashboard — this key is only needed when you call the relay API
                directly (for example, to ingest events).
              </AlertDescription>
            </Alert>
            <Button onClick={() => router.push("/endpoints")}>Continue to dashboard</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="mx-auto flex max-w-md flex-col gap-6 pt-16">
      <div>
        <h1 className="text-2xl font-semibold">relay</h1>
        <p className="mt-1 text-muted-foreground">Webhook delivery — endpoints, topics, and deliveries.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Create your account</CardTitle>
          <CardDescription>Sets up a new organization and logs you in to the dashboard.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={signup} className="flex flex-col gap-3">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="organizationName">Organization name</Label>
              <Input
                id="organizationName"
                value={organizationName}
                onChange={(e) => setOrganizationName(e.target.value)}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                minLength={8}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <Button type="submit" disabled={busy || !organizationName || !email || !password}>
              {busy ? "Creating account…" : "Create account"}
            </Button>
          </form>
          {error && <p className="mt-3 text-sm text-destructive">{error}</p>}
        </CardContent>
      </Card>

      <p className="text-center text-sm text-muted-foreground">
        Already have an account?{" "}
        <Link href="/" className="underline underline-offset-4 hover:text-foreground">
          Log in
        </Link>
      </p>
    </div>
  );
}
