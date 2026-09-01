import type { Metadata } from "next";
import { Nav } from "@/components/Nav";
import "./globals.css";

export const metadata: Metadata = {
  title: "Relay Dashboard",
  description: "Manage endpoints, topics and deliveries for relay.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className="h-full antialiased">
      <body className="min-h-full flex flex-col">
        <Nav />
        <main className="flex-1 p-6">{children}</main>
      </body>
    </html>
  );
}
