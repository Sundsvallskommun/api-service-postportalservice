	ALTER TABLE user RENAME COLUMN name TO username;

    CREATE INDEX IDX_USER_USERNAME ON user (username);
