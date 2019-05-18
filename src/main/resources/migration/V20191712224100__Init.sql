CREATE TABLE IF NOT EXISTS admin_credentials(
  user_id INT NOT NULL,
  full_name VARCHAR(512),
  created_at TIMESTAMP,
  deleted_at TIMESTAMP,
  PRIMARY KEY(user_id)
);

CREATE TABLE IF NOT EXISTS countries(
  country_name VARCHAR(512),
  PRIMARY KEY(country_name)
);