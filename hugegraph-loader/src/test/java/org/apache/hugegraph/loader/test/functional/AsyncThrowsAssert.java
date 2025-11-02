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

package org.apache.hugegraph.loader.test.functional;

import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

import org.apache.hugegraph.testutil.Assert;

public class AsyncThrowsAssert extends  Assert {

    public static void assertThrows(Class<? extends Throwable> throwable,
                                    Assert.ThrowableRunnable runnable,
                                    Consumer<Throwable> exceptionConsumer) {
        boolean fail = false;
        try {
            runnable.run();
            fail = true;
        } catch (Throwable e) {
            if (CompletionException.class.isInstance(e)) {
                e=e.getCause();
            }
            if (!throwable.isInstance(e)) {
                Assert.fail(String.format(
                        "Bad exception type %s(expected %s)",
                        e.getClass().getName(), throwable.getName()));
            }
            exceptionConsumer.accept(e);
        }
        if (fail) {
            Assert.fail(String.format(
                    "No exception was thrown(expected %s)",
                    throwable.getName()));
        }
    }
    public static Throwable assertThrows(Class<? extends Throwable> throwable,
                                         ThrowableRunnable runnable) {
        assertThrows(throwable, runnable, e -> {
            System.err.println(e);
        });
        return null;
    }

}
