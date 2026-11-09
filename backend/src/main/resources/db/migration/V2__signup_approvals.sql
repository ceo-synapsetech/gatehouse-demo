CREATE TABLE IF NOT EXISTS app_users (
    username VARCHAR(120) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    account_role VARCHAR(20) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    unit VARCHAR(32),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    approval_status VARCHAR(16),
    approved_by_603 BOOLEAN DEFAULT FALSE,
    approved_by_second BOOLEAN DEFAULT FALSE
);

ALTER TABLE app_users ADD COLUMN IF NOT EXISTS approval_status VARCHAR(16);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS approved_by_603 BOOLEAN;
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS approved_by_second BOOLEAN;
