/*
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

package org.apache.hugegraph.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import org.apache.hugegraph.entity.load.EdgeMapping;
import org.apache.hugegraph.loader.util.JsonUtil;

@MappedTypes(value = {EdgeMapping.class})
@MappedJdbcTypes(value = {JdbcType.VARCHAR})
public class EdgeMappingTypeHandler extends BaseTypeHandler<Set<EdgeMapping>> {

    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement,
                                    int columnIndex, Set<EdgeMapping> mappings,
                                    JdbcType jdbcType) throws SQLException {
        preparedStatement.setString(columnIndex, JsonUtil.toJson(mappings));
    }

    @Override
    public Set<EdgeMapping> getNullableResult(ResultSet resultSet,
                                              String columnName)
                                              throws SQLException {
        String json = resultSet.getString(columnName);
        return JsonUtil.convertSet(json, EdgeMapping.class);
    }

    @Override
    public Set<EdgeMapping> getNullableResult(ResultSet resultSet,
                                              int columnIndex)
                                              throws SQLException {
        String json = resultSet.getString(columnIndex);
        return JsonUtil.convertSet(json, EdgeMapping.class);
    }

    @Override
    public Set<EdgeMapping> getNullableResult(
                            CallableStatement callableStatement,
                            int columnIndex) throws SQLException {
        String json = callableStatement.getString(columnIndex);
        return JsonUtil.convertSet(json, EdgeMapping.class);
    }
}
