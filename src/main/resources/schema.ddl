USE admintools;

CREATE TABLE IF NOT EXISTS player_login (
  id         CHAR(36),
  name       VARCHAR(16),
  ip_address INT UNSIGNED NOT NULL,
  time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS player_ip_punish (
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

CREATE TABLE IF NOT EXISTS player_ip_punish_reverse (
  punish_id     INT,
  ip_address    INT UNSIGNED,
  sender_id     CHAR(36),
  creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (punish_id) REFERENCES player_ip_punish (id),
  FOREIGN KEY (ip_address) REFERENCES player_login (ip_address),
  FOREIGN KEY (sender_id) REFERENCES player_login (id)
);

CREATE TABLE IF NOT EXISTS player_punish (
  id            INT                   AUTO_INCREMENT,
  banned_id     CHAR(36),
  sender_id     CHAR(36),
  reason        VARCHAR(100) NOT NULL,
  creation_date TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expiry_date   TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (banned_id) REFERENCES player_login (id),
  FOREIGN KEY (sender_id) REFERENCES player_login (id)
);

CREATE TABLE IF NOT EXISTS player_punish_reverse (
  punish_id     INT,
  banned_id     CHAR(36),
  sender_id     CHAR(36),
  reason        VARCHAR(100) NOT NULL,
  creation_date TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expiry_date   TIMESTAMP,
  FOREIGN KEY (punish_id) REFERENCES player_punish (id),
  FOREIGN KEY (banned_id) REFERENCES player_login (id),
  FOREIGN KEY (sender_id) REFERENCES player_login (id)
);