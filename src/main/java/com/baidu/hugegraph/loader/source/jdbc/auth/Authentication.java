package com.baidu.hugegraph.loader.source.jdbc.auth;

import com.baidu.hugegraph.loader.source.jdbc.JDBCSource;


public interface Authentication {

    String auth(JDBCSource source) throws Exception;
}
