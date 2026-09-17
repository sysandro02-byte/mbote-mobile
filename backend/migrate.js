const fs = require('node:fs');
const path = require('node:path');
const { Pool } = require('pg');
require('dotenv').config();

const INTEGER_TYPES = new Set(['int2', 'int4', 'int8']);

function q(value) {
  return `"${String(value).replace(/"/g, '""')}"`;
}

async function getColumnType(db, table, column) {
  const result = await db.query(
    `SELECT data_type, udt_name
       FROM information_schema.columns
      WHERE table_schema = 'public' AND table_name = $1 AND column_name = $2`,
    [table, column],
  );
  return result.rows[0] || null;
}

async function getForeignKeysTo(db, targetTable) {
  const result = await db.query(`
    SELECT DISTINCT
      tc.table_name,
      kcu.column_name,
      tc.constraint_name,
      rc.delete_rule,
      rc.update_rule
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
      ON tc.constraint_name = kcu.constraint_name
     AND tc.constraint_schema = kcu.constraint_schema
    JOIN information_schema.constraint_column_usage ccu
      ON ccu.constraint_name = tc.constraint_name
     AND ccu.constraint_schema = tc.constraint_schema
    JOIN information_schema.referential_constraints rc
      ON rc.constraint_name = tc.constraint_name
     AND rc.constraint_schema = tc.constraint_schema
    WHERE tc.constraint_type = 'FOREIGN KEY'
      AND tc.table_schema = 'public'
      AND ccu.table_schema = 'public'
      AND ccu.table_name = $1
      AND ccu.column_name = 'id'
  `, [targetTable]);
  return result.rows;
}

function deterministicUuidSql(table, expression) {
  const prefix = String(table).replace(/'/g, "''");
  return `md5('mbote:${prefix}:' || (${expression})::text)::uuid`;
}

async function migrateLegacyIntegerEntity(db, table) {
  const idType = await getColumnType(db, table, 'id');
  if (!idType || idType.udt_name === 'uuid') return false;
  if (!INTEGER_TYPES.has(idType.udt_name)) {
    throw new Error(`Type ${table}.id historique non pris en charge: ${idType.udt_name}`);
  }

  const refs = await getForeignKeysTo(db, table);
  console.log(`[mbote-db] Migration UUID: ${table}.id=${idType.udt_name}, ${refs.length} FK(s)...`);

  await db.query('BEGIN');
  try {
    for (const ref of refs) {
      const refType = await getColumnType(db, ref.table_name, ref.column_name);
      if (!refType) continue;
      if (refType.udt_name !== 'uuid' && !INTEGER_TYPES.has(refType.udt_name)) {
        throw new Error(`Référence historique non prise en charge: ${ref.table_name}.${ref.column_name} (${refType.udt_name})`);
      }
      await db.query(`ALTER TABLE ${q(ref.table_name)} DROP CONSTRAINT IF EXISTS ${q(ref.constraint_name)}`);
    }

    // Convert referencing columns first while their old integer values are still available.
    // ALTER COLUMN TYPE preserves local PK/UNIQUE/index definitions, unlike dropping columns.
    for (const ref of refs) {
      const refType = await getColumnType(db, ref.table_name, ref.column_name);
      if (!refType || refType.udt_name === 'uuid') continue;
      await db.query(
        `ALTER TABLE ${q(ref.table_name)} ALTER COLUMN ${q(ref.column_name)} TYPE UUID USING ${deterministicUuidSql(table, q(ref.column_name))}`,
      );
    }

    await db.query(
      `ALTER TABLE ${q(table)} ALTER COLUMN id TYPE UUID USING ${deterministicUuidSql(table, 'id')}`,
    );
    await db.query(`ALTER TABLE ${q(table)} ALTER COLUMN id SET DEFAULT uuid_generate_v4()`);

    for (const ref of refs) {
      const currentType = await getColumnType(db, ref.table_name, ref.column_name);
      if (!currentType || currentType.udt_name !== 'uuid') continue;
      const deleteRule = ['CASCADE', 'SET NULL', 'SET DEFAULT', 'RESTRICT', 'NO ACTION'].includes(ref.delete_rule)
        ? ref.delete_rule
        : 'NO ACTION';
      const updateRule = ['CASCADE', 'SET NULL', 'SET DEFAULT', 'RESTRICT', 'NO ACTION'].includes(ref.update_rule)
        ? ref.update_rule
        : 'NO ACTION';
      await db.query(
        `ALTER TABLE ${q(ref.table_name)} ADD CONSTRAINT ${q(ref.constraint_name)} FOREIGN KEY (${q(ref.column_name)}) REFERENCES ${q(table)}(id) ON UPDATE ${updateRule} ON DELETE ${deleteRule}`,
      );
    }

    await db.query('COMMIT');
    console.log(`[mbote-db] ${table}.id migré vers UUID.`);
    return true;
  } catch (error) {
    await db.query('ROLLBACK');
    throw error;
  }
}

async function migrateLegacyIntegerUsers(db) {
  const usersId = await getColumnType(db, 'users', 'id');
  if (!usersId || usersId.udt_name === 'uuid') return;
  if (!INTEGER_TYPES.has(usersId.udt_name)) {
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
    await db.query(`INSERT INTO legacy_user_uuid_map (legacy_id) SELECT id::BIGINT FROM users ON CONFLICT (legacy_id) DO NOTHING`);

    const refs = await getForeignKeysTo(db, 'users');
    for (const ref of refs) {
      const type = await getColumnType(db, ref.table_name, ref.column_name);
      if (!type || type.udt_name === 'uuid') continue;
      if (!INTEGER_TYPES.has(type.udt_name)) throw new Error(`Référence historique non prise en charge: ${ref.table_name}.${ref.column_name} (${type.udt_name})`);
      await db.query(`ALTER TABLE ${q(ref.table_name)} DROP CONSTRAINT IF EXISTS ${q(ref.constraint_name)}`);
      await db.query(`ALTER TABLE ${q(ref.table_name)} ADD COLUMN IF NOT EXISTS ${q(`${ref.column_name}__uuid`)} UUID`);
      await db.query(`UPDATE ${q(ref.table_name)} t SET ${q(`${ref.column_name}__uuid`)} = m.user_uuid FROM legacy_user_uuid_map m WHERE t.${q(ref.column_name)}::BIGINT = m.legacy_id AND t.${q(`${ref.column_name}__uuid`)} IS NULL`);
      await db.query(`ALTER TABLE ${q(ref.table_name)} DROP COLUMN ${q(ref.column_name)}`);
      await db.query(`ALTER TABLE ${q(ref.table_name)} RENAME COLUMN ${q(`${ref.column_name}__uuid`)} TO ${q(ref.column_name)}`);
    }

    await db.query('ALTER TABLE users ADD COLUMN IF NOT EXISTS id__uuid UUID');
    await db.query(`UPDATE users u SET id__uuid = m.user_uuid FROM legacy_user_uuid_map m WHERE u.id::BIGINT = m.legacy_id AND u.id__uuid IS NULL`);
    await db.query('ALTER TABLE users DROP CONSTRAINT IF EXISTS users_pkey');
    await db.query('ALTER TABLE users DROP COLUMN id');
    await db.query('ALTER TABLE users RENAME COLUMN id__uuid TO id');
    await db.query('ALTER TABLE users ALTER COLUMN id SET DEFAULT uuid_generate_v4()');
    await db.query('ALTER TABLE users ALTER COLUMN id SET NOT NULL');
    await db.query('ALTER TABLE users ADD PRIMARY KEY (id)');

    for (const ref of refs) {
      const exists = await getColumnType(db, ref.table_name, ref.column_name);
      if (!exists || exists.udt_name !== 'uuid') continue;
      const deleteRule = ['CASCADE', 'SET NULL', 'SET DEFAULT', 'RESTRICT', 'NO ACTION'].includes(ref.delete_rule) ? ref.delete_rule : 'NO ACTION';
      const updateRule = ['CASCADE', 'SET NULL', 'SET DEFAULT', 'RESTRICT', 'NO ACTION'].includes(ref.update_rule) ? ref.update_rule : 'NO ACTION';
      await db.query(`ALTER TABLE ${q(ref.table_name)} ADD CONSTRAINT ${q(ref.constraint_name)} FOREIGN KEY (${q(ref.column_name)}) REFERENCES users(id) ON UPDATE ${updateRule} ON DELETE ${deleteRule}`);
    }

    await db.query('COMMIT');
    console.log('[mbote-db] Migration des identifiants utilisateurs vers UUID terminée.');
  } catch (error) {
    await db.query('ROLLBACK');
    throw error;
  }
}

function uuidPrimaryKeyTablesFromSchema(schema) {
  const tables = [];
  const re = /CREATE\s+TABLE\s+IF\s+NOT\s+EXISTS\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\([\s\S]*?\bid\s+UUID\s+PRIMARY\s+KEY\b/gi;
  let match;
  while ((match = re.exec(schema)) !== null) tables.push(match[1]);
  return [...new Set(tables)];
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
    const schema = fs.readFileSync(path.join(__dirname, 'schema.sql'), 'utf8');
    await db.query('CREATE EXTENSION IF NOT EXISTS "uuid-ossp"');
    await migrateLegacyIntegerUsers(db);

    // Existing production databases predate the UUID schema for several entities.
    // Discover UUID PK tables from schema.sql so future entities are covered without
    // another one-off migration. Each conversion is transactional and keeps FK rules.
    const uuidTables = uuidPrimaryKeyTablesFromSchema(schema).filter((table) => table !== 'users');
    for (const table of uuidTables) await migrateLegacyIntegerEntity(db, table);

    await db.query(schema);
    const required = ['users', 'auth_challenges', 'app_content'];
    const check = await db.query(
      `SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ANY($1::text[])`,
      [required],
    );
    const found = new Set(check.rows.map((row) => row.table_name));
    const missing = required.filter((table) => !found.has(table));
    if (missing.length) throw new Error(`Migration incomplète: ${missing.join(', ')}`);

    for (const table of uuidPrimaryKeyTablesFromSchema(schema)) {
      const idType = await getColumnType(db, table, 'id');
      if (idType && idType.udt_name !== 'uuid') throw new Error(`Migration invalide: ${table}.id=${idType.udt_name}, UUID attendu.`);
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
