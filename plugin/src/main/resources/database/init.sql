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

CREATE TABLE IF NOT EXISTS player_flatten_selection (
  player_uuid     TEXT NOT NULL PRIMARY KEY,
  pos1_world      TEXT,
  pos1_x          INTEGER,
  pos1_y          INTEGER,
  pos1_z          INTEGER,
  pos2_world      TEXT,
  pos2_x          INTEGER,
  pos2_y          INTEGER,
  pos2_z          INTEGER,
  chest_blocks    TEXT,
  preserve_ores   INTEGER NOT NULL DEFAULT 0,
  pickup_items    INTEGER NOT NULL DEFAULT 1,
  auto_deposit    INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS fakeplayer_flatten_task (
  fakeplayer_uuid TEXT NOT NULL PRIMARY KEY,
  world           TEXT NOT NULL,
  min_x           INTEGER NOT NULL,
  max_x           INTEGER NOT NULL,
  min_y           INTEGER NOT NULL,
  max_y           INTEGER NOT NULL,
  min_z           INTEGER NOT NULL,
  max_z           INTEGER NOT NULL,
  preserve_ores   INTEGER NOT NULL DEFAULT 0,
  pickup_items    INTEGER NOT NULL DEFAULT 1,
  auto_deposit    INTEGER NOT NULL DEFAULT 1,
  chest_locations TEXT,
  total_blocks    INTEGER NOT NULL DEFAULT 0,
  cleared_blocks  INTEGER NOT NULL DEFAULT 0,
  created_at      INTEGER NOT NULL
);