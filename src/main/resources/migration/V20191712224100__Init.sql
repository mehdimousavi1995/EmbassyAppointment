CREATE TABLE IF NOT EXISTS admin_credentials(
  user_id INT NOT NULL,
  created_at TIMESTAMP,
  deleted_at TIMESTAMP,
  PRIMARY KEY(user_id)
)