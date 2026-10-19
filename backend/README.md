# GhostPocket Cloudflare D1 Backend 👻⚡

A high-performance, edge-first REST API built with [Cloudflare Workers](https://workers.cloudflare.com/), [Hono](https://hono.dev/), and [Cloudflare D1](https://developers.cloudflare.com/d1/) (Edge SQLite).

---

## 🏗️ Architecture

- **Database**: Cloudflare D1 (SQLite database: `ghostpocket-db`, ID: `961fef79-259d-4685-aaa0-04210e014e18`)
- **API Framework**: Hono v4 (TypeScript)
- **Deployment**: Cloudflare Workers
- **Sync Protocol**:
  - `POST /api/sync/push`: Atomic batch `INSERT ... ON CONFLICT DO UPDATE` into D1
  - `GET /api/sync/pull?since={timestamp}`: Fast indexed queries for incremental two-way sync
  - `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/google`: Flexible user auth & Google ID token verification

---

## 🚀 Setup & Deployment

### 1. Initialize Tables in Cloudflare D1

Run the schema initialization against your remote D1 database:

```bash
cd backend
npx wrangler d1 execute ghostpocket-db --remote --file=./schema.sql
```

*(For local offline dev testing, execute: `npx wrangler d1 execute ghostpocket-db --local --file=./schema.sql`)*

### 2. Run Local Development Server

```bash
cd backend
npm run dev
# Starts on http://127.0.0.1:8787
```

### 3. Deploy Worker to Cloudflare

Ensure you are logged into Wrangler (`npx wrangler login`), then run:

```bash
npm run deploy
```

This will print your live worker URL:
`https://ghostpocket-backend.<your-subdomain>.workers.dev`

---

## 📱 Connecting Android App

In Android app:
- Default emulator URL points to `http://10.0.2.2:8787` (local host).
- When deploying to production, update the `baseUrl` in `D1ApiClient.kt` or through the settings screen to your live Cloudflare Worker URL.
