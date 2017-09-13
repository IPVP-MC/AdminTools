USE admintools;

CREATE TABLE IF NOT EXISTS player_login (
  id         CHAR(36),
  name       VARCHAR(16),
  ip_address INT UNSIGNED NOT NULL,
  time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS player_ip_ban (
  id            INT                   AUTO_INCREMENT,
  ip_address    INT UNSIGNED,
  sender_id     CHAR(36),
  reason        VARCHAR(100) NOT NULL,
  creation_date TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expiry_date   TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (ip_address) REFERENCES player_login (ip_address),
  FOREIGN KEY (sender_id) REFERENCES player_login (id)
);

CREATE TABLE IF NOT EXISTS player_ip_unban (
  ban_id        INT,
  ip_address    INT UNSIGNED,
  sender_id     CHAR(36),
  creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (ban_id) REFERENCES player_ip_ban (id),
  FOREIGN KEY (ip_address) REFERENCES player_login (ip_address),
  FOREIGN KEY (sender_id) REFERENCES player_login (id)
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
  expiry_date   TIMESTAMP,
  type          ENUM ('ban', 'mute'),
  PRIMARY KEY (id),
  FOREIGN KEY (banned_id) REFERENCES player_login (id),
  FOREIGN KEY (sender_id) REFERENCES player_login (id)
);

CREATE TABLE IF NOT EXISTS player_punish_reverse (
  punish_id     INT          NOT NULL,
  banned_id     CHAR(36)     NOT NULL,
  sender_id     CHAR(36)     NOT NULL,
  reason        VARCHAR(100) NOT NULL,
  creation_date TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
  FOREIGN KEY (punish_id) REFERENCES player_punish (id),
  FOREIGN KEY (banned_id) REFERENCES player_login (id),
  FOREIGN KEY (sender_id) REFERENCES player_login (id)
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