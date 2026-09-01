"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/apiClient";
import { setStoredApiKey } from "@/lib/apiKey";
import { setStoredSession } from "@/lib/session";
import { useIsConnected } from "@/lib/useConnection";
import type { AuthResponse } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export default function LoginPage() {
  const router = useRouter();
  const connected = useIsConnected();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [existingKey, setExistingKey] = useState("");
  const [showKeyForm, setShowKeyForm] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (connected) {
      router.push("/endpoints");
    }
  }, [connected, router]);

  if (connected) {
    return null;
  }

  async function login(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const auth = await api.post<AuthResponse>("/auth/login", { email, password });
      setStoredSession({ organizationId: auth.organizationId, organizationName: auth.organizationName, email: auth.email });
      router.push("/endpoints");
    } catch (err) {
      setError(err instanceof ApiError ? "Invalid email or password." : "Something went wrong.");
    } finally {
      setBusy(false);
    }
  }

  function connectWithExistingKey(e: React.FormEvent) {
    e.preventDefault();
    setStoredApiKey(existingKey.trim());
    router.push("/endpoints");
  }

  return (
    <div className="mx-auto flex max-w-md flex-col gap-6 pt-16">
      <div>
        <h1 className="text-2xl font-semibold">relay</h1>
        <p className="mt-1 text-muted-foreground">Webhook delivery — endpoints, topics, and deliveries.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Log in</CardTitle>
          <CardDescription>Access your organization&apos;s dashboard.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={login} className="flex flex-col gap-3">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <Button type="submit" disabled={busy || !email || !password}>
              {busy ? "Logging in…" : "Log in"}
            </Button>
          </form>
          {error && <p className="mt-3 text-sm text-destructive">{error}</p>}
        </CardContent>
      </Card>

      <p className="text-center text-sm text-muted-foreground">
        New to relay?{" "}
        <Link href="/signup" className="underline underline-offset-4 hover:text-foreground">
          Create an account
        </Link>
      </p>

      <div className="text-center">
        <button
          type="button"
          onClick={() => setShowKeyForm((v) => !v)}
          className="text-sm text-muted-foreground underline underline-offset-4 hover:text-foreground"
        >
          {showKeyForm ? "Hide" : "Or connect with an API key"}
        </button>
      </div>

      {showKeyForm && (
        <Card>
          <CardHeader>
            <CardTitle>Connect with an API key</CardTitle>
            <CardDescription>For organizations provisioned directly through the API.</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={connectWithExistingKey} className="flex flex-col gap-3">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="apiKey">API key</Label>
                <Input
                  id="apiKey"
                  className="font-mono text-sm"
                  placeholder="relay_..."
                  value={existingKey}
                  onChange={(e) => setExistingKey(e.target.value)}
                  required
                />
              </div>
              <Button type="submit" variant="outline" disabled={!existingKey}>
                Connect
              </Button>
            </form>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
