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

package org.apache.hugegraph.spark.connector.constant;

import java.nio.charset.Charset;

import com.google.common.base.Charsets;

public final class Constants {

    public static final Charset CHARSET = Charsets.UTF_8;

    public static final String HTTP_PREFIX = "http://";

    public static final String HTTPS_PREFIX = "https://";

    public static final String TRUST_STORE_PATH = "conf/hugegraph.truststore";

    public static final String COLON_STR = ":";

    public static final String COMMA_STR = ",";

    public static final int STATUS_UNAUTHORIZED = 401;

    public static final int VERTEX_ID_LIMIT = 128;

    public static final String[] SEARCH_LIST = new String[]{":", "!"};

    public static final String[] TARGET_LIST = new String[]{"`:", "`!"};

    public static final String TIMESTAMP = "timestamp";

    public static final String TIME_ZONE = "GMT+8";

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final String DEFAULT_TRUST_STORE_TOKEN = "hugegraph";

    public static final String CONNECTOR_HOME_PATH = "connector.home.path";

    public static final String HTTPS_SCHEMA = "https";

    public static final String HTTP_SCHEMA = "http";

    public static final String DEFAULT_HOST = "localhost";

    public static final int DEFAULT_PORT = 8080;

    public static final String DEFAULT_GRAPH = "hugegraph";

    public static final String DEFAULT_PROTOCOL = HTTP_SCHEMA;

    public static final int DEFAULT_TIMEOUT = 60;

    public static final int DEFAULT_BATCH_SIZE = 500;
}
