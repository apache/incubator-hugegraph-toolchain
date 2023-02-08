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

package org.apache.hugegraph.structure.schema;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.hugegraph.exception.InvalidOperationException;

public class BuilderProxy<T> implements InvocationHandler {

    private boolean finished;
    private T builder;
    private T proxy;

    @SuppressWarnings("unchecked")
    public BuilderProxy(T builder) {
        this.finished = false;
        this.builder = builder;
        this.proxy = (T) Proxy.newProxyInstance(builder.getClass().getClassLoader(),
                                                builder.getClass().getInterfaces(), this);
    }

    public T proxy() {
        return this.proxy;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
                         throws Throwable {
        if (this.finished) {
            throw new InvalidOperationException(
                      "Can't access builder which is completed");
        }
        // The result may be equal this.builder, like method `asText`
        Object result = method.invoke(this.builder, args);
        if (result == this.builder) {
            result = this.proxy;
        } else {
            this.finished = true;
        }
        return result;
    }
}
