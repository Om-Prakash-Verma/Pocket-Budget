# GhostPocket Cloudflare D1 Backend 👻⚡

A high-performance, edge-first REST API built with [Cloudflare Workers](https://workers.cloudflare.com/), [Hono v4](https://hono.dev/), and [Cloudflare D1](https://developers.cloudflare.com/d1/) (Serverless Edge SQLite).

---

## 🏗️ Architecture

- **Database**: Cloudflare D1 (SQLite database: `ghostpocket-db`, ID: `961fef79-259d-4685-aaa0-04210e014e18`)
- **API Framework**: Hono v4 (TypeScript)
- **Deployment**: Cloudflare Workers
- **Live Worker URL**: `https://ghostpocket-backend.omprakashverma-in-fda.workers.dev`

### Cost & Performance Optimizations
1. **Unified Single-Roundtrip 2-Way Sync (`POST /api/sync`)**:
   - Pushes client's dirty records and pulls remote updates in a **single atomic request**.
   - Reduces Worker invocations by 50% compared to separate push/pull cycles.
   - Executes batch operations with D1's atomic `db.batch()` to prevent partial writes.
2. **Only-When-Dirty Gating**:
   - The Android client only invokes `/api/sync` when local mutations exist or periodic intervals elapse.
3. **Smart Header Caching**:
   - Edge responses include appropriate `Cache-Control` directives.
4. **Credential Security**:
   - Passwords hashed with SHA-256 with strict verification and zero raw credentials stored.

---

## 🔌 API Endpoints

### Authentication
- `POST /api/auth/register`
  - Body: `{ "email": "...", "password": "...", "displayName": "..." }`
- `POST /api/auth/login`
  - Body: `{ "email": "...", "password": "..." }`
- `POST /api/auth/google`
  - Body: `{ "idToken": "..." }`

### Data Synchronization
- `POST /api/sync`
  - Body:
    ```json
    {
      "transactions": [...],
      "friends": [...],
      "ledgers": [...],
      "categories": [...],
      "initialBudgetPaise": 500000,
      "lastSyncTimestamp": 1729330000000
    }
    ```
  - Response:
    ```json
    {
      "success": true,
      "pushedCount": 5,
      "pulledTransactions": [...],
      "pulledFriends": [...],
      "pulledLedgers": [...],
      "pulledCategories": [...],
      "initialBudgetPaise": 500000,
      "serverTimestamp": 1729330500000
    }
    ```

---

## 🚀 Setup & Deployment

### 1. Initialize Tables in Cloudflare D1

Run schema initialization against your remote D1 database:

```bash
cd backend
npx wrangler d1 execute ghostpocket-db --remote --file=./schema.sql
```

*(For local dev testing: `npx wrangler d1 execute ghostpocket-db --local --file=./schema.sql`)*

### 2. Run Local Development Server

```bash
cd backend
npm run dev
# Starts locally on http://127.0.0.1:8787
```

### 3. Deploy Worker to Cloudflare

```bash
npm run deploy
```

---

## 📱 Connecting the Android App

In the Android app:
- Default emulator URL points to `http://10.0.2.2:8787` for local development.
- Production builds point to:
  `https://ghostpocket-backend.omprakashverma-in-fda.workers.dev`
