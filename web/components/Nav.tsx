"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { api } from "@/lib/apiClient";
import { clearStoredApiKey } from "@/lib/apiKey";
import { clearStoredSession, getStoredSession } from "@/lib/session";
import { useIsConnected } from "@/lib/useConnection";
import { Button } from "@/components/ui/button";

const LINKS = [
  { href: "/endpoints", label: "Endpoints" },
  { href: "/topics", label: "Topics" },
  { href: "/deliveries", label: "Deliveries" },
];

export function Nav() {
  const pathname = usePathname();
  const router = useRouter();
  const connected = useIsConnected();

  if (!connected) return null;

  async function disconnect() {
    if (getStoredSession()) {
      try {
        await api.post("/auth/logout");
      } catch {
        // best-effort — clearing local state below still logs the user out of this browser
      }
      clearStoredSession();
    }
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
