-- Flyway V1 Migration Script
-- IMPORTANT: Since `spring.flyway.baseline-on-migrate=true` is set,
-- if you connect to your existing local database, Flyway will SKIP this file
-- and mark version 1 as already applied. This is the intended behavior for existing databases.

-- If you are starting from a completely fresh database (e.g. using the new docker-compose),
-- you should paste your initial schema (e.g. from pg_dump) here so Flyway can create the tables,
-- otherwise `spring.jpa.hibernate.ddl-auto=validate` will fail on an empty database.

-- Alternatively, for local development with a fresh database, you can temporarily 
-- set `spring.jpa.hibernate.ddl-auto=update` to let Hibernate generate the schema,
-- then switch back to `validate` and start using Flyway for V2 onwards.
