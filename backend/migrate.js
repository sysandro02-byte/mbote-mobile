const fs = require('node:fs');
const path = require('node:path');
const { Pool } = require('pg');
require('dotenv').config();

async function getColumnType(db, table, column) {
  const result = await db.query(
    `SELECT data_type, udt_name
       FROM information_schema.columns
      WHERE table_schema = 'public' AND table_name = $1 AND column_name = $2`,
    [table, column],
  );
  return result.rows[0] || null;
}

async function migrateLegacyIntegerUsers(db) {
  const usersId = await getColumnType(db, 'users', 'id');
  if (!usersId || usersId.udt_name === 'uuid') return;

  if (!['int2', 'int4', 'int8'].includes(usersId.udt_name)) {
    throw new Error(`Type users.id historique non pris en charge: ${usersId.udt_name}`);
  }

  console.log(`[mbote-db] Ancien schéma détecté: users.id=${usersId.udt_name}. Migration UUID...`);
  await db.query('BEGIN');
  try {
    await db.query('CREATE EXTENSION IF NOT EXISTS "uuid-ossp"');
    await db.query(`
      CREATE TABLE IF NOT EXISTS legacy_user_uuid_map (
        legacy_id BIGINT PRIMARY KEY,
        user_uuid UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),
        migrated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
      )
    `);
    await db.query(`
      INSERT INTO legacy_user_uuid_map (legacy_id)
      SELECT id::BIGINT FROM users
      ON CONFLICT (legacy_id) DO NOTHING
    `);

    const refs = await db.query(`
      SELECT DISTINCT
        tc.table_name,
        kcu.column_name,
        tc.constraint_name
      FROM information_schema.table_constraints tc
      JOIN information_schema.key_column_usage kcu
        ON tc.constraint_name = kcu.constraint_name
       AND tc.constraint_schema = kcu.constraint_schema
      JOIN information_schema.constraint_column_usage ccu
        ON ccu.constraint_name = tc.constraint_name
       AND ccu.constraint_schema = tc.constraint_schema
      WHERE tc.constraint_type = 'FOREIGN KEY'
        AND tc.table_schema = 'public'
        AND ccu.table_schema = 'public'
        AND ccu.table_name = 'users'
        AND ccu.column_name = 'id'
    `);

    for (const ref of refs.rows) {
      const table = ref.table_name.replace(/"/g, '""');
      const column = ref.column_name.replace(/"/g, '""');
      const constraint = ref.constraint_name.replace(/"/g, '""');
      const type = await getColumnType(db, ref.table_name, ref.column_name);
      if (!type || type.udt_name === 'uuid') continue;
      if (!['int2', 'int4', 'int8'].includes(type.udt_name)) {
        throw new Error(`Référence historique non prise en charge: ${ref.table_name}.${ref.column_name} (${type.udt_name})`);
      }
      await db.query(`ALTER TABLE "${table}" DROP CONSTRAINT IF EXISTS "${constraint}"`);
      await db.query(`ALTER TABLE "${table}" ADD COLUMN IF NOT EXISTS "${column}__uuid" UUID`);
      await db.query(`
        UPDATE "${table}" t
           SET "${column}__uuid" = m.user_uuid
          FROM legacy_user_uuid_map m
         WHERE t."${column}"::BIGINT = m.legacy_id
           AND t."${column}__uuid" IS NULL
      `);
      await db.query(`ALTER TABLE "${table}" DROP COLUMN "${column}"`);
      await db.query(`ALTER TABLE "${table}" RENAME COLUMN "${column}__uuid" TO "${column}"`);
    }

    await db.query('ALTER TABLE users ADD COLUMN IF NOT EXISTS id__uuid UUID');
    await db.query(`
      UPDATE users u
         SET id__uuid = m.user_uuid
        FROM legacy_user_uuid_map m
       WHERE u.id::BIGINT = m.legacy_id
         AND u.id__uuid IS NULL
    `);
    await db.query('ALTER TABLE users DROP CONSTRAINT IF EXISTS users_pkey');
    await db.query('ALTER TABLE users DROP COLUMN id');
    await db.query('ALTER TABLE users RENAME COLUMN id__uuid TO id');
    await db.query('ALTER TABLE users ALTER COLUMN id SET DEFAULT uuid_generate_v4()');
    await db.query('ALTER TABLE users ALTER COLUMN id SET NOT NULL');
    await db.query('ALTER TABLE users ADD PRIMARY KEY (id)');

    for (const ref of refs.rows) {
      const table = ref.table_name.replace(/"/g, '""');
      const column = ref.column_name.replace(/"/g, '""');
      const constraint = ref.constraint_name.replace(/"/g, '""');
      const exists = await getColumnType(db, ref.table_name, ref.column_name);
      if (!exists || exists.udt_name !== 'uuid') continue;
      await db.query(`ALTER TABLE "${table}" ADD CONSTRAINT "${constraint}" FOREIGN KEY ("${column}") REFERENCES users(id) ON DELETE CASCADE`);
    }

    await db.query('COMMIT');
    console.log('[mbote-db] Migration des identifiants utilisateurs vers UUID terminée.');
  } catch (error) {
    await db.query('ROLLBACK');
    throw error;
  }
}

async function main() {
  if (!process.env.DATABASE_URL) throw new Error('DATABASE_URL est requis.');
  const db = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: process.env.DATABASE_SSL === 'false' ? false : { rejectUnauthorized: false },
    max: 1,
    connectionTimeoutMillis: Number(process.env.DATABASE_CONNECT_TIMEOUT_MS || 10000),
  });
  try {
    await migrateLegacyIntegerUsers(db);
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
    const finalUsersId = await getColumnType(db, 'users', 'id');
    if (!finalUsersId || finalUsersId.udt_name !== 'uuid') {
      throw new Error(`Migration invalide: users.id=${finalUsersId?.udt_name || 'absent'}, UUID attendu.`);
    }
    console.log('[mbote-db] Schéma PostgreSQL synchronisé.');
  } finally {
    await db.end();
  }
}

main().catch((error) => {
  console.error('[mbote-db] Échec migration:', error);
  process.exit(1);
});
