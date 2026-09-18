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
    const exists = await db.query("SELECT to_regclass('public.job_applications') AS table_name");
    if (!exists.rows[0]?.table_name) return;

    // CREATE TABLE IF NOT EXISTS does not upgrade legacy tables. Keep these
    // additions nullable so existing production rows remain valid.
    await db.query('ALTER TABLE job_applications ADD COLUMN IF NOT EXISTS job_id UUID REFERENCES job_offers(id) ON DELETE CASCADE');
    await db.query('ALTER TABLE job_applications ADD COLUMN IF NOT EXISTS applicant_id UUID REFERENCES users(id) ON DELETE CASCADE');
    await db.query('ALTER TABLE job_applications ADD COLUMN IF NOT EXISTS cv_url TEXT');
    await db.query("ALTER TABLE job_applications ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'SUBMITTED'");
    await db.query('ALTER TABLE job_applications ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()');
    console.log('[mbote-db] Pré-migration job_applications historique terminée.');
  } finally {
    await db.end();
  }
}

main().catch((error) => {
  console.error('[mbote-db] Échec pré-migration:', error);
  process.exit(1);
});
