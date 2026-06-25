CREATE TABLE IF NOT EXISTS fakeplayer (
  id            INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  name          TEXT NOT NULL UNIQUE,
  uuid          TEXT NOT NULL UNIQUE,
  creator_uuid  TEXT,
  skin          TEXT,
  settings      TEXT
);

CREATE TABLE IF NOT EXISTS ref_fakeplayer_owner (
  owner_uuid       TEXT NOT NULL,
  fakeplayer_uuid  TEXT NOT NULL,
  PRIMARY KEY (owner_uuid, fakeplayer_uuid),
  FOREIGN KEY (fakeplayer_uuid) REFERENCES fakeplayer(uuid) ON DELETE CASCADE
);