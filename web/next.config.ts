import type { NextConfig } from "next";

const RELAY_API_URL = process.env.RELAY_API_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/api/relay/:path*",
        destination: `${RELAY_API_URL}/:path*`,
      },
    ];
  },
};

export default nextConfig;
