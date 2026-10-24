-- =====================================================================
-- StudentHub: one-time database setup (run as a MySQL admin, e.g. root)
--
--   mysql -u root -p < database/01_create_database.sql
--   (or open it in MySQL Workbench and press the lightning-bolt "Execute")
--
-- It creates the database and an application user that can only touch
-- the studenthub schema. The backend connects as this user, never as root.
-- CHANGE THE PASSWORD below before running it, and put the same value in
-- backend/.env as DB_PASSWORD.

CREATE DATABASE IF NOT EXISTS studenthub
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'studenthub_app'@'localhost' IDENTIFIED BY 'CHANGE_ME_to_a_long_random_password';
CREATE USER IF NOT EXISTS 'studenthub_app'@'%'         IDENTIFIED BY 'CHANGE_ME_to_a_long_random_password';

-- Data access plus the DDL Flyway needs to create and evolve the schema.
-- For stricter production setups, run migrations with a separate user and
-- grant the runtime user only SELECT, INSERT, UPDATE, DELETE.
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES
  ON studenthub.* TO 'studenthub_app'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES
  ON studenthub.* TO 'studenthub_app'@'%';

FLUSH PRIVILEGES;

-- The tables are created automatically by Flyway the first time the backend
-- starts (backend/src/main/resources/db/migration). database/schema.sql is a
-- reference copy for reading or for MySQL Workbench's reverse-engineer view.
