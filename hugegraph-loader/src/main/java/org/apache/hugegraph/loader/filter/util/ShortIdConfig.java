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

package org.apache.hugegraph.loader.filter.util;

import org.apache.hugegraph.loader.exception.LoadException;
import org.apache.hugegraph.structure.constant.DataType;

import com.beust.jcommander.IStringConverter;

public class ShortIdConfig {

    private String vertexLabel;
    private String idFieldName;
    private DataType idFieldType;
    private String primaryKeyField;

    private long labelID;

    public String getVertexLabel() {
        return vertexLabel;
    }

    public String getIdFieldName() {
        return idFieldName;
    }

    public DataType getIdFieldType() {
        return idFieldType;
    }

    public void setPrimaryKeyField(String primaryKeyField) {
        this.primaryKeyField = primaryKeyField;
    }

    public String getPrimaryKeyField() {
        return primaryKeyField;
    }

    public long getLabelID() {
        return labelID;
    }

    public void setLabelID(long labelID) {
        this.labelID = labelID;
    }

    public static class ShortIdConfigConverter implements IStringConverter<ShortIdConfig> {

        @Override
        public ShortIdConfig convert(String s) {
            String[] sp = s.split(":");
            ShortIdConfig config = new ShortIdConfig();
            config.vertexLabel = sp[0];
            config.idFieldName = sp[1];
            String a = DataType.BYTE.name();
            switch (sp[2]) {
                case "boolean":
                    config.idFieldType = DataType.BOOLEAN;
                    break;
                case "byte":
                    config.idFieldType = DataType.BYTE;
                    break;
                case "int":
                    config.idFieldType = DataType.INT;
                    break;
                case "long":
                    config.idFieldType = DataType.LONG;
                    break;
                case "float":
                    config.idFieldType = DataType.FLOAT;
                    break;
                case "double":
                    config.idFieldType = DataType.DOUBLE;
                    break;
                case "text":
                    config.idFieldType = DataType.TEXT;
                    break;
                case "blob":
                    config.idFieldType = DataType.BLOB;
                    break;
                case "date":
                    config.idFieldType = DataType.DATE;
                    break;
                case "uuid":
                    config.idFieldType = DataType.UUID;
                    break;
                default:
                    throw new LoadException("unknow type " + sp[2]);
            }
            return config;
        }
    }
}
