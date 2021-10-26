package com.baidu.hugegraph.loader.reader.jdbc;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.reader.line.Line;
import com.baidu.hugegraph.loader.source.jdbc.JDBCSource;
import com.baidu.hugegraph.util.Log;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class JDBCFetcher extends Fetcher {
    private static final Logger LOG = Log.logger(JDBCFetcher.class);
    private Statement stmt = null;
    private ResultSet result = null;

    public JDBCFetcher(JDBCSource source) throws SQLException {
        super(source);
    }

    @Override
    public String[] readHeader() {
        return null;
    }

    @Override
    public void readPrimaryKey()  {

    }

    @Override
    public void close() {
        try {
            if (result != null && !result.isClosed()) result.close();
        } catch (SQLException e) {
            LOG.warn("Failed to close 'ResultSet'", e);
        }
        try {
            if (stmt != null && !stmt.isClosed()) stmt.close();
        } catch (SQLException e) {
            LOG.warn("Failed to close 'Statement'", e);
        }
        try {
            if (this.conn != null && !conn.isClosed()) this.conn.close();
        } catch (SQLException e) {
            LOG.warn("Failed to close 'Connection'", e);
        }
    }

    long offSet = 0;
    boolean start = false;
    boolean done = false;
    String[] columns = null;

    @Override
    public  List<Line> nextBatch() throws SQLException {
        if (!start) {
            stmt = this.conn.createStatement();
            //use fields instead of * , from json ?
            result = stmt.executeQuery("select * from " + source.table());
            result.setFetchSize(source.batchSize());
            ResultSetMetaData metaData = result.getMetaData();
            columns = new String[metaData.getColumnCount()];
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String fieldName = metaData.getColumnName(i);
                columns[i - 1] = fieldName.replaceFirst(source.table() + ".",
                        "");
            }
            this.source.header(columns);
            start = true;
        }
        if (done) {
            LOG.warn("no other data");
            return null;
        }
        ArrayList<Line> lines = new ArrayList<>(source.batchSize());
        for (int j = 0; j < source.batchSize(); j++) {
            if (result.next()) {
                int n = this.columns.length;
                Object[] values = new Object[n];
                for (int i = 1; i <= n; i++) {
                    Object value = result.getObject(i);
                    if (value == null) {
                        value = Constants.NULL_STR;
                    }
                    values[i - 1] = value;
                }
                String rawLine = StringUtils.join(values, Constants.COMMA_STR);
                Line line = new Line(rawLine, this.columns, values);
                lines.add(line);
            } else {
                done = true;
                break;
            }
        }
        return lines;
    }
}
