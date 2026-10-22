import { Hono } from 'hono';
import { cors } from 'hono/cors';

type Bindings = {
  DB: D1Database;
};

const app = new Hono<{ Bindings: Bindings }>();

app.use('*', cors());

// Health check
app.get('/', (c) => c.text('GhostPocket Cloudflare D1 Backend 👻⚡'));

// -------------------------------------------------------------
// Authentication Helpers
// -------------------------------------------------------------
function extractUserId(c: any): string {
  const authHeader = c.req.header('Authorization');
  if (!authHeader) return 'guest_user';
  const parts = authHeader.split(' ');
  return parts.length === 2 ? parts[1] : 'guest_user';
}

// -------------------------------------------------------------
// Auth Endpoints: Register & Login
// -------------------------------------------------------------
app.post('/api/auth/register', async (c) => {
  const { email, password } = await c.req.json<{ email: string; password: string }>();
  if (!email || !password) {
    return c.json({ error: 'Missing email or password' }, 400);
  }

  const userId = `usr_${crypto.randomUUID()}`;
  const now = Date.now();

  try {
    const existing = await c.env.DB.prepare('SELECT id FROM users WHERE email = ?').bind(email).first();
    if (existing) {
      return c.json({ error: 'Email already exists' }, 400);
    }

    await c.env.DB.prepare(
      'INSERT INTO users (id, email, password_hash, created_at) VALUES (?, ?, ?, ?)'
    ).bind(userId, email, password, now).run();

    return c.json({ token: userId, userId, email });
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

app.post('/api/auth/login', async (c) => {
  const { email, password } = await c.req.json<{ email: string; password: string }>();
  if (!email || !password) {
    return c.json({ error: 'Missing email or password' }, 400);
  }

  try {
    const user = await c.env.DB.prepare(
      'SELECT id, email, password_hash FROM users WHERE email = ?'
    ).bind(email).first<{ id: string; email: string; password_hash: string }>();

    if (!user || user.password_hash !== password) {
      return c.json({ error: 'Invalid email or password' }, 401);
    }

    return c.json({ token: user.id, userId: user.id, email: user.email });
  } catch (err: any) {
    return c.json({ error: err.message }, 500);
  }
});

// -------------------------------------------------------------
// BEST OPTIMIZATION: UNIFIED 2-WAY SYNC IN A SINGLE REQUEST
// (Combines Push + Pull into 1 call -> Cuts Worker usage by 50%)
// -------------------------------------------------------------
app.post('/api/sync', async (c) => {
  const userId = extractUserId(c);
  if (!userId || userId === 'guest_user') {
    return c.json({ error: 'Unauthorized' }, 401);
  }

  const reqBody = await c.req.json<{
    sinceTimestamp?: number;
    transactions?: any[];
    friends?: any[];
    ledgers?: any[];
    categories?: any[];
    budgets?: any[];
  }>();

  const statements: D1PreparedStatement[] = [];
  const now = Date.now();

  // 1. Process Push writes (if any)
  if (reqBody.friends && reqBody.friends.length > 0) {
    for (const f of reqBody.friends) {
      statements.push(
        c.env.DB.prepare(
          `INSERT INTO friends (id, user_id, name, phone, updated_at, is_deleted)
           VALUES (?, ?, ?, ?, ?, ?)
           ON CONFLICT(id) DO UPDATE SET
             name = excluded.name,
             phone = excluded.phone,
             updated_at = excluded.updated_at,
             is_deleted = excluded.is_deleted`
        ).bind(f.id, userId, f.name, f.phone || null, f.updatedAt || now, f.isDeleted ? 1 : 0)
      );
    }
  }

  if (reqBody.ledgers && reqBody.ledgers.length > 0) {
    for (const l of reqBody.ledgers) {
      statements.push(
        c.env.DB.prepare(
          `INSERT INTO friend_ledgers (id, user_id, friend_id, amount_paise, direction, note, timestamp, is_settled, updated_at, is_deleted)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
           ON CONFLICT(id) DO UPDATE SET
             amount_paise = excluded.amount_paise,
             direction = excluded.direction,
             note = excluded.note,
             timestamp = excluded.timestamp,
             is_settled = excluded.is_settled,
             updated_at = excluded.updated_at,
             is_deleted = excluded.is_deleted`
        ).bind(
          l.id,
          userId,
          l.friendId,
          l.amountPaise,
          l.direction,
          l.note || null,
          l.timestamp || now,
          l.isSettled ? 1 : 0,
          l.updatedAt || now,
          l.isDeleted ? 1 : 0
        )
      );
    }
  }

  if (reqBody.categories && reqBody.categories.length > 0) {
    for (const cat of reqBody.categories) {
      statements.push(
        c.env.DB.prepare(
          `INSERT INTO categories (name, user_id, icon_key, budget_paise, updated_at, is_deleted)
           VALUES (?, ?, ?, ?, ?, ?)
           ON CONFLICT(name, user_id) DO UPDATE SET
             icon_key = excluded.icon_key,
             budget_paise = excluded.budget_paise,
             updated_at = excluded.updated_at,
             is_deleted = excluded.is_deleted`
        ).bind(
          cat.name,
          userId,
          cat.iconKey || 'ghost',
          cat.budgetPaise || 0,
          cat.updatedAt || now,
          cat.isDeleted ? 1 : 0
        )
      );
    }
  }

  if (reqBody.transactions && reqBody.transactions.length > 0) {
    for (const tx of reqBody.transactions) {
      statements.push(
        c.env.DB.prepare(
          `INSERT INTO transactions (id, user_id, amount_paise, type, category, timestamp, note, payment_method, updated_at, is_deleted)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
           ON CONFLICT(id) DO UPDATE SET
             amount_paise = excluded.amount_paise,
             type = excluded.type,
             category = excluded.category,
             timestamp = excluded.timestamp,
             note = excluded.note,
             payment_method = excluded.payment_method,
             updated_at = excluded.updated_at,
             is_deleted = excluded.is_deleted`
        ).bind(
          tx.id,
          userId,
          tx.amountPaise,
          tx.type,
          tx.category,
          tx.timestamp || now,
          tx.note || null,
          tx.paymentMethod || null,
          tx.updatedAt || now,
          tx.isDeleted ? 1 : 0
        )
      );
    }
  }

  if (reqBody.budgets && reqBody.budgets.length > 0) {
    for (const b of reqBody.budgets) {
      statements.push(
        c.env.DB.prepare(
          `INSERT INTO user_settings (id, user_id, budget_paise, updated_at)
           VALUES (?, ?, ?, ?)
           ON CONFLICT(id, user_id) DO UPDATE SET
             budget_paise = excluded.budget_paise,
             updated_at = excluded.updated_at`
        ).bind(b.id, userId, b.budgetPaise || 0, b.updatedAt || now)
      );
    }
  }

  if (statements.length > 0) {
    await c.env.DB.batch(statements);
  }

  // 2. Concurrently Pull any updates modified since sinceTimestamp
  const since = reqBody.sinceTimestamp || 0;

  const [txRes, friendsRes, ledgersRes, categoriesRes, budgetsRes] = await Promise.all([
    c.env.DB.prepare(
      'SELECT * FROM transactions WHERE user_id = ? AND updated_at > ?'
    ).bind(userId, since).all(),
    c.env.DB.prepare(
      'SELECT * FROM friends WHERE user_id = ? AND updated_at > ?'
    ).bind(userId, since).all(),
    c.env.DB.prepare(
      'SELECT * FROM friend_ledgers WHERE user_id = ? AND updated_at > ?'
    ).bind(userId, since).all(),
    c.env.DB.prepare(
      'SELECT * FROM categories WHERE user_id = ? AND updated_at > ?'
    ).bind(userId, since).all(),
    c.env.DB.prepare(
      'SELECT * FROM user_settings WHERE user_id = ? AND updated_at > ?'
    ).bind(userId, since).all(),
  ]);

  // Set edge cache header to prevent repeat network traffic
  c.header('Cache-Control', 'private, no-cache');

  return c.json({
    success: true,
    processedCount: statements.length,
    serverTimestamp: now,
    remoteUpdates: {
      transactions: txRes.results.map((tx: any) => ({
        id: tx.id,
        amountPaise: tx.amount_paise,
        type: tx.type,
        category: tx.category,
        timestamp: tx.timestamp,
        note: tx.note,
        paymentMethod: tx.payment_method,
        updatedAt: tx.updated_at,
        isDeleted: tx.is_deleted === 1
      })),
      friends: friendsRes.results.map((f: any) => ({
        id: f.id,
        name: f.name,
        phone: f.phone,
        updatedAt: f.updated_at,
        isDeleted: f.is_deleted === 1
      })),
      ledgers: ledgersRes.results.map((l: any) => ({
        id: l.id,
        friendId: l.friend_id,
        amountPaise: l.amount_paise,
        direction: l.direction,
        note: l.note,
        timestamp: l.timestamp,
        isSettled: l.is_settled === 1,
        updatedAt: l.updated_at,
        isDeleted: l.is_deleted === 1
      })),
      categories: categoriesRes.results.map((cat: any) => ({
        name: cat.name,
        iconKey: cat.icon_key,
        budgetPaise: cat.budget_paise,
        updatedAt: cat.updated_at,
        isDeleted: cat.is_deleted === 1
      })),
      budgets: budgetsRes.results.map((b: any) => ({
        id: b.id,
        budgetPaise: b.budget_paise,
        updatedAt: b.updated_at
      }))
    }
  });
});

export default app;
