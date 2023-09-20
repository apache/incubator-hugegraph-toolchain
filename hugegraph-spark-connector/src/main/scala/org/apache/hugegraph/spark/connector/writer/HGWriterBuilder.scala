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

package org.apache.hugegraph.spark.connector.writer

import org.apache.hugegraph.spark.connector.options.HGOptions
import org.apache.spark.sql.connector.write.{BatchWrite, SupportsOverwrite, SupportsTruncate, WriteBuilder}
import org.apache.spark.sql.sources.Filter
import org.apache.spark.sql.types.StructType
import org.slf4j.LoggerFactory

class HGWriterBuilder(schema: StructType, hgOptions: HGOptions)
  extends WriteBuilder with SupportsOverwrite with SupportsTruncate {

  private val LOG = LoggerFactory.getLogger(this.getClass)

  override def buildForBatch(): BatchWrite = {
    new HGBatchWriter(schema, hgOptions)
  }

  override def overwrite(filters: Array[Filter]): WriteBuilder = {
    new HGWriterBuilder(schema, hgOptions)
  }
}
