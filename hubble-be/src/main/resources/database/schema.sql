CREATE TABLE IF NOT EXISTS `user_info` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(48) NOT NULL,
    `locale` VARCHAR(20) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`username`)
);

CREATE TABLE IF NOT EXISTS `graph_connection` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(48) NOT NULL,
    `graph` VARCHAR(48) NOT NULL,
    `host` VARCHAR(48) NOT NULL DEFAULT 'localhost',
    `port` INT NOT NULL DEFAULT '8080',
    `username` VARCHAR(48),
    `password` VARCHAR(48),
    `enabled` BOOLEAN NOT NULL DEFAULT true,
    `disable_reason` VARCHAR(65535) NOT NULL DEFAULT '',
    `create_time` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`name`),
    UNIQUE (`graph`, `host`, `port`)
);

CREATE TABLE IF NOT EXISTS `execute_history` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `conn_id` INT NOT NULL,
    `execute_type` TINYINT NOT NULL,
    `content` VARCHAR(65535) NOT NULL,
    `execute_status` TINYINT NOT NULL,
    `duration` LONG NOT NULL,
    `create_time` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`)
);
CREATE INDEX IF NOT EXISTS `execute_history_conn_id` ON `execute_history`(`conn_id`);

CREATE TABLE IF NOT EXISTS `gremlin_collection` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `conn_id` INT NOT NULL,
    `name` VARCHAR(48) NOT NULL,
    `content` VARCHAR(65535) NOT NULL,
    `create_time` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`conn_id`, `name`)
);
CREATE INDEX IF NOT EXISTS `gremlin_collection_conn_id` ON `gremlin_collection`(`conn_id`);
