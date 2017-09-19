USE admintools;

CREATE TABLE IF NOT EXISTS player_login (
  id         CHAR(36),
  name       VARCHAR(16),
  ip_address INT UNSIGNED NOT NULL,
  time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE VIEW player_latest_login AS
  SELECT *
  FROM player_login
  ORDER BY time DESC
  LIMIT 1;

CREATE OR REPLACE VIEW player_related_ip_login AS
  SELECT DISTINCT
    l1.id   id1,
    l2.id   id2,
    l1.name name1,
    l2.name name2,
    l1.time time1,
    l2.time time2
  FROM player_login l1
    JOIN player_login l2
      ON l1.ip_address = l2.ip_address
         AND l1.id <> l2.id;

CREATE TABLE IF NOT EXISTS player_ip_ban (
  id            INT                   AUTO_INCREMENT,
  ip_address    INT UNSIGNED,
  sender_id     CHAR(36),
  reason        VARCHAR(100) NOT NULL,
  creation_date TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expiry_date   TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS player_ip_unban (
  ban_id        INT,
  ip_address    INT UNSIGNED,
  sender_id     CHAR(36),
  creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (ban_id) REFERENCES player_ip_ban (id)
    ON DELETE CASCADE
);

CREATE OR REPLACE VIEW player_active_ip_ban AS
  SELECT
    id,
    INET_NTOA(ip_address) ip_address,
    sender_id,
    reason,
    creation_date,
    expiry_date
  FROM player_ip_ban
  WHERE expiry_date > CURRENT_TIMESTAMP
        AND id NOT IN (
    SELECT ban_id
    FROM player_ip_unban
    WHERE player_ip_unban.ban_id = player_ip_ban.id
  );

CREATE TABLE IF NOT EXISTS player_punish (
  id            INT                   AUTO_INCREMENT,
  banned_id     CHAR(36),
  sender_id     CHAR(36),
  reason        VARCHAR(100) NOT NULL,
  creation_date TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expiry_date   TIMESTAMP    NULL,
  type          ENUM ('ban', 'mute', 'warn'),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS player_punish_reverse (
  punish_id     INT       NOT NULL,
  banned_id     CHAR(36)  NOT NULL,
  sender_id     CHAR(36)  NOT NULL,
  reason        VARCHAR(100),
  creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (punish_id) REFERENCES player_punish (id)
    ON DELETE CASCADE
);

CREATE OR REPLACE VIEW player_active_punishment AS
  SELECT *
  FROM player_punish
  WHERE expiry_date > CURRENT_TIMESTAMP
        AND id NOT IN (
    SELECT punish_id
    FROM player_punish_reverse
    WHERE player_punish_reverse.punish_id = player_punish.id
  );