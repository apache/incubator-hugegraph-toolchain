/*
 * Copyright 2017 HugeGraph Authors
 *
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

package com.baidu.hugegraph.loader.source.jdbc;

import com.baidu.hugegraph.loader.reader.jdbc.JDBCUtil;

public enum JDBCVendor {

    MYSQL {
        @Override
        public String buildGetHeaderSql(String database, String table) {
            return String.format("SELECT COLUMN_NAME " +
                                 "FROM INFORMATION_SCHEMA.COLUMNS " +
                                 "WHERE TABLE_SCHEMA = %s " +
                                 "AND TABLE_NAME = %s;",
                                 JDBCUtil.escape(this, database),
                                 JDBCUtil.escape(this, table));
        }
    },

    POSTGRESQL {
        @Override
        public String buildGetHeaderSql(String database, String table) {
            return String.format("SELECT COLUMN_NAME " +
                                 "FROM INFORMATION_SCHEMA.COLUMNS " +
                                 "WHERE TABLE_CATALOG = %s " +
                                 "AND TABLE_NAME = %s;",
                                 JDBCUtil.escape(this, database),
                                 JDBCUtil.escape(this, table));
        }
    },

    ORACLE {
        @Override
        public String buildGetHeaderSql(String database, String table) {
            return String.format("SELECT COLUMN_NAME " +
                                 "FROM USER_TAB_COLUMNS " +
                                 "WHERE TABLE_NAME = %s;",
                                 JDBCUtil.escape(this, table));
        }
    },

    SQL_SERVER {
        @Override
        public String buildGetHeaderSql(String database, String table) {
            return String.format("SELECT COLUMN_NAME " +
                                 "FROM INFORMATION_SCHEMA.COLUMNS " +
                                 "WHERE TABLE_NAME = N%s;",
                                 JDBCUtil.escape(this, table));
        }
    };

    public abstract String buildGetHeaderSql(String database, String table);
}
