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

package org.apache.hugegraph.controller;

import java.util.List;

import org.springframework.util.StringUtils;

import org.apache.hugegraph.common.Identifiable;
import org.apache.hugegraph.common.Mergeable;
import org.apache.hugegraph.util.EntityUtil;
import org.apache.hugegraph.util.Ex;

public abstract class BaseController {

    public static final String ORDER_ASC = "asc";
    public static final String ORDER_DESC = "desc";

    public void checkIdSameAsBody(Object id, Identifiable newEntity) {
        Ex.check(newEntity.getId() != null, () -> id.equals(newEntity.getId()),
                 "common.param.path-id-should-same-as-body",
                 id, newEntity.getId());
    }

    public void checkParamsNotEmpty(String name, String value,
                                    boolean creating) {
        if (creating) {
            Ex.check(!StringUtils.isEmpty(value),
                     "common.param.cannot-be-null-or-empty", name);
        } else {
            // The default null and user-passed null indicate no update
            Ex.check(value == null || !value.isEmpty(),
                     "common.param.cannot-be-empty", name);
        }
    }

    public void checkParamsNotEmpty(String name, List<?> values) {
        Ex.check(values != null && !values.isEmpty(),
                 "common.param.cannot-be-null-or-empty", name);
    }

    public <T extends Mergeable> T mergeEntity(T oldEntity, T newEntity) {
        return EntityUtil.merge(oldEntity, newEntity);
    }
}
