"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/apiClient";
import { getStoredApiKey, setStoredApiKey } from "@/lib/apiKey";
import type { Organization } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export default function ConnectPage() {
  const router = useRouter();
  const [orgName, setOrgName] = useState("");
  const [existingKey, setExistingKey] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (getStoredApiKey()) {
      router.push("/endpoints");
    }
  }, [router]);

  async function createOrganization(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const org = await api.post<Organization>("/organizations", { name: orgName });
      setStoredApiKey(org.apiKey);
      router.push("/endpoints");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong.");
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
          <CardTitle>Create a new organization</CardTitle>
          <CardDescription>Get an API key to start registering endpoints.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={createOrganization} className="flex flex-col gap-3">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="orgName">Organization name</Label>
              <Input id="orgName" value={orgName} onChange={(e) => setOrgName(e.target.value)} required />
            </div>
            <Button type="submit" disabled={busy || !orgName}>
              Create and get an API key
            </Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Connect with an existing key</CardTitle>
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

      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}
