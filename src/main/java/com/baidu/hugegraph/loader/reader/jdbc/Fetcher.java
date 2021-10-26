package com.baidu.hugegraph.loader.reader.jdbc;

import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.reader.line.Line;
import com.baidu.hugegraph.loader.source.jdbc.JDBCSource;
import com.baidu.hugegraph.util.Log;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public abstract class Fetcher {


    protected JDBCSource source;
    protected Connection conn;
    private static final Logger LOG = Log.logger(Fetcher.class);

    public Fetcher(JDBCSource source) throws SQLException {
        this.source = source;
        this.conn = this.connect();
    }

    public JDBCSource getSource() {
        return source;
    }

    public Connection getConn() {
        return conn;
    }

    private Connection connect() throws SQLException {
        String url = this.getSource().vendor().buildUrl(this.source);
        if (url == null) {
            throw new LoadException("Invalid url !");
        }
        LOG.info("Connect to database {}", url);
        String driverName = this.source.driver();
        String username = this.source.username();
        String password = this.source.password();
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException e) {
            throw new LoadException("Invalid driver class '%s'", e, driverName);
        }
        return DriverManager.getConnection(url ,
                username,
                password);
    }

    ;

    abstract String[] readHeader() throws SQLException;

    abstract void readPrimaryKey() throws SQLException;

    abstract void close();

    abstract List<Line> nextBatch()  throws SQLException;
}
