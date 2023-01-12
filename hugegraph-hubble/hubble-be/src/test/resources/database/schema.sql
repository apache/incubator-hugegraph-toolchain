/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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
    `timeout` INT NOT NULL,
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
    `async_id` LONG NOT NULL DEFAULT 0,
    `execute_type` TINYINT NOT NULL,
    `content` VARCHAR(65535) NOT NULL,
    `execute_status` TINYINT NOT NULL,
    `async_status` TINYINT NOT NULL DEFAULT 0,
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

CREATE TABLE IF NOT EXISTS `file_mapping` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `conn_id` INT NOT NULL,
    `job_id` INT NOT NULL DEFAULT 0,
    `name` VARCHAR(128) NOT NULL,
    `path` VARCHAR(256) NOT NULL,
    `total_lines` LONG NOT NULL,
    `total_size` LONG NOT NULL,
    `file_status` TINYINT NOT NULL DEFAULT 0,
    `file_setting` VARCHAR(65535) NOT NULL,
    `vertex_mappings` VARCHAR(65535) NOT NULL,
    `edge_mappings` VARCHAR(65535) NOT NULL,
    `load_parameter` VARCHAR(65535) NOT NULL,
    `create_time` DATETIME(6) NOT NULL,
    `update_time` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`conn_id`, `job_id`, `name`)
);
CREATE INDEX IF NOT EXISTS `file_mapping_conn_id` ON `file_mapping`(`conn_id`);

CREATE TABLE IF NOT EXISTS `load_task` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `conn_id` INT NOT NULL,
    `job_id` INT NOT NULL DEFAULT 0,
    `file_id` INT NOT NULL,
    `file_name` VARCHAR(128) NOT NULL,
    `options` VARCHAR(65535) NOT NULL,
    `vertices` VARCHAR(512) NOT NULL,
    `edges` VARCHAR(512) NOT NULL,
    `file_total_lines` LONG NOT NULL,
    `load_status` TINYINT NOT NULL,
    `file_read_lines` LONG NOT NULL,
    `last_duration` LONG NOT NULL,
    `curr_duration` LONG NOT NULL,
    `create_time` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `job_manager` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `conn_id` INT NOT NULL DEFAULT 0,
    `job_name` VARCHAR(100) NOT NULL DEFAULT '',
    `job_remarks` VARCHAR(200) NOT NULL DEFAULT '',
    `job_size` LONG NOT NULL DEFAULT 0,
    `job_status` TINYINT NOT NULL DEFAULT 0,
    `job_duration` LONG NOT NULL DEFAULT 0,
    `update_time` DATETIME(6) NOT NULL,
    `create_time` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`job_name`, `conn_id`)
);

CREATE TABLE IF NOT EXISTS `async_task` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `conn_id` INT NOT NULL DEFAULT 0,
    `task_id` INT NOT NULL DEFAULT 0,
    `task_name` VARCHAR(100) NOT NULL DEFAULT '',
    `task_reason` VARCHAR(200) NOT NULL DEFAULT '',
    `task_type` TINYINT NOT NULL DEFAULT 0,
    `algorithm_name` VARCHAR(48) NOT NULL DEFAULT '',
    `task_content` VARCHAR(65535) NOT NULL DEFAULT '',
    `task_status` TINYINT NOT NULL DEFAULT 0,
    `task_duration` LONG NOT NULL DEFAULT 0,
    `create_time`  DATETIME(6)  NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE INDEX IF NOT EXISTS `load_task_conn_id` ON `load_task`(`conn_id`);
CREATE INDEX IF NOT EXISTS `load_task_file_id` ON `load_task`(`file_id`);
