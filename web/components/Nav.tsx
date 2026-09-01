"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { clearStoredApiKey, getStoredApiKey } from "@/lib/apiKey";
import { useSyncExternalStore } from "react";
import { Button } from "@/components/ui/button";

const LINKS = [
  { href: "/endpoints", label: "Endpoints" },
  { href: "/topics", label: "Topics" },
  { href: "/deliveries", label: "Deliveries" },
];

function noopSubscribe() {
  return () => {};
}

function useIsConnected() {
  // No cross-tab subscription needed — this only changes via our own connect/disconnect
  // actions, which already trigger a re-render (navigation) that re-reads the snapshot.
  return useSyncExternalStore(
    noopSubscribe,
    () => Boolean(getStoredApiKey()),
    () => false
  );
}

export function Nav() {
  const pathname = usePathname();
  const router = useRouter();
  const connected = useIsConnected();

  if (!connected) return null;

  function disconnect() {
    clearStoredApiKey();
    router.push("/");
  }

  return (
    <nav className="flex items-center justify-between border-b px-6 py-4">
      <div className="flex items-center gap-6">
        <span className="font-semibold">relay</span>
        {LINKS.map((link) => (
          <Link
            key={link.href}
            href={link.href}
            className={
              pathname?.startsWith(link.href)
                ? "font-medium underline underline-offset-4"
                : "text-muted-foreground hover:text-foreground"
            }
          >
            {link.label}
          </Link>
        ))}
      </div>
      <Button variant="ghost" size="sm" onClick={disconnect}>
        Disconnect
      </Button>
    </nav>
  );
}
