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

package org.apache.hugegraph.loader.reader;

import org.apache.hugegraph.loader.executor.LoadContext;
import org.apache.hugegraph.loader.mapping.InputStruct;
import org.apache.hugegraph.loader.progress.InputProgress;

public abstract class AbstractReader implements InputReader {

    protected InputProgress oldProgress;
    protected InputProgress newProgress;

    public void progress(LoadContext context, InputStruct struct) {
        this.oldProgress = context.oldProgress().get(struct.id());
        if (this.oldProgress == null) {
            this.oldProgress = new InputProgress(struct);
        }
        // Update loading vertex/edge mapping
        this.newProgress = context.newProgress().addStruct(struct);
    }
}
