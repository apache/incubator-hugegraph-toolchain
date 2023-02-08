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

package org.apache.hugegraph.util;

import java.util.Arrays;
import java.util.List;

/**
 * Copied from HugeGraph(<a href="https://github.com/hugegraph/hugegraph">...</a>)
 */
public class SplicingIdGenerator {

    private static volatile SplicingIdGenerator instance;

    public static SplicingIdGenerator instance() {
        if (instance == null) {
            synchronized (SplicingIdGenerator.class) {
                if (instance == null) {
                    instance = new SplicingIdGenerator();
                }
            }
        }
        return instance;
    }

    /*
     * The following defines can't be java regex special characters:
     * "\^$.|?*+()[{"
     * See: http://www.regular-expressions.info/characters.html
     */
    private static final char ESCAPE = '`';
    private static final char IDS_SPLITOR = '>';
    private static final char ID_SPLITOR = ':';
    private static final char NAME_SPLITOR = '!';

    public static final String ESCAPE_STR = String.valueOf(ESCAPE);
    public static final String IDS_SPLITOR_STR = String.valueOf(IDS_SPLITOR);
    public static final String ID_SPLITOR_STR = String.valueOf(ID_SPLITOR);

    /****************************** id generate ******************************/

    /**
     * Concat multiple ids into one composite id with IDS_SPLITOR
     * @param ids the string id values to be contacted
     * @return    contacted string value
     */
    public static String concat(String... ids) {
        // NOTE: must support string id when using this method
        return IdUtil.escape(IDS_SPLITOR, ESCAPE, ids);
    }

    /**
     * Split a composite id into multiple ids with IDS_SPLITOR
     * @param ids the string id value to be split
     * @return    split string values
     */
    public static String[] split(String ids) {
        return IdUtil.unescape(ids, IDS_SPLITOR_STR, ESCAPE_STR);
    }

    /**
     * Concat property values with NAME_SPLITOR
     * @param values the property values to be contacted
     * @return       contacted string value
     */
    public static String concatValues(List<?> values) {
        // Convert the object list to string array
        int valuesSize = values.size();
        String[] parts = new String[valuesSize];
        for (int i = 0; i < valuesSize; i++) {
            parts[i] = values.get(i).toString();
        }
        return IdUtil.escape(NAME_SPLITOR, ESCAPE, parts);
    }

    /**
     * Concat property values with NAME_SPLITOR
     * @param values the property values to be contacted
     * @return       contacted string value
     */
    public static String concatValues(Object... values) {
        return concatValues(Arrays.asList(values));
    }
}
