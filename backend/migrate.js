const fs = require('node:fs');
const path = require('node:path');
const { Pool } = require('pg');
require('dotenv').config();

async function main() {
  if (!process.env.DATABASE_URL) throw new Error('DATABASE_URL est requis.');
  const db = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: process.env.DATABASE_SSL === 'false' ? false : { rejectUnauthorized: false },
    max: 1,
    connectionTimeoutMillis: Number(process.env.DATABASE_CONNECT_TIMEOUT_MS || 10000),
  });
  try {
    const schema = fs.readFileSync(path.join(__dirname, 'schema.sql'), 'utf8');
    await db.query(schema);
    const required = ['users', 'auth_challenges', 'app_content'];
    const check = await db.query(
      `SELECT table_name FROM information_schema.tables
       WHERE table_schema = 'public' AND table_name = ANY($1::text[])`,
      [required],
    );
    const found = new Set(check.rows.map((row) => row.table_name));
    const missing = required.filter((table) => !found.has(table));
    if (missing.length) throw new Error(`Migration incomplète: ${missing.join(', ')}`);
    console.log('[mbote-db] Schéma PostgreSQL synchronisé.');
  } finally {
    await db.end();
  }
}

main().catch((error) => {
  console.error('[mbote-db] Échec migration:', error);
  process.exit(1);
});
