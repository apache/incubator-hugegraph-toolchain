///*
// * Copyright 2017 HugeGraph Authors
// *
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements. See the NOTICE file distributed with this
// * work for additional information regarding copyright ownership. The ASF
// * licenses this file to You under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// * License for the specific language governing permissions and limitations
// * under the License.
// */
//
//package com.baidu.hugegraph.loader.reader.line;
//
//import com.baidu.hugegraph.util.E;
//
//public class TableLine extends Line {
//
//    private final String[] names;
//    private final Object[] values;
//
//    public TableLine(String rawLine, String[] names, Object[] values) {
//        super(rawLine);
//        E.checkArgumentNotNull(names, "The names can't be null");
//        E.checkArgumentNotNull(values, "The values can't be null");
//        E.checkArgument(names.length == values.length,
//                        "The length of names %s should be same as values %s");
//        this.names = names;
//        this.values = values;
//    }
//
//    public final String[] names() {
//        return this.names;
//    }
//
//    public final Object[] values() {
//        return this.values;
//    }
//}
