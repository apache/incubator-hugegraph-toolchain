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

package org.apache.hugegraph.spark.connector

import org.apache.spark.sql.{SaveMode, SparkSession}

/*
// Define schema
schema.propertyKey("name").asText().ifNotExist().create();
schema.propertyKey("age").asInt().ifNotExist().create();
schema.propertyKey("city").asText().ifNotExist().create();
schema.propertyKey("weight").asDouble().ifNotExist().create();
schema.propertyKey("lang").asText().ifNotExist().create();
schema.propertyKey("date").asText().ifNotExist().create();
schema.propertyKey("price").asDouble().ifNotExist().create();

schema.vertexLabel("person")
        .properties("name", "age", "city")
//            .primaryKeys("name")
        .useCustomizeStringId()
        .nullableKeys("age", "city")
        .ifNotExist()
        .create();

schema.vertexLabel("software")
        .properties("name", "lang", "price")
        .primaryKeys("name")
//            .useCustomizeStringId()
        .ifNotExist()
        .create();

schema.edgeLabel("knows")
        .sourceLabel("person")
        .targetLabel("person")
        .properties("date", "weight")
        .ifNotExist()
        .create();

schema.edgeLabel("created")
        .sourceLabel("person")
        .targetLabel("software")
        .properties("date", "weight")
        .ifNotExist()
        .create();

 */
object TestSink {

  // TODO transfer to test
  def main(args: Array[String]): Unit = {
    val sparkSession = SparkSession.builder()
      .master("local[*]")
      .appName(this.getClass.getSimpleName)
      .config("spark.ui.port", "19099")
      .getOrCreate()

    insertVertices1(sparkSession)
    insertVertices2(sparkSession)

    testInsertEdge1(sparkSession)
    testInsertEdge2(sparkSession)

    sparkSession.stop()
  }

  def insertVertices1(sparkSession: SparkSession): Unit = {
    val df = sparkSession.createDataFrame(Seq(
      Tuple3("marko", 29, "Beijing"),
      Tuple3("vadas", 27, "HongKong"),
      Tuple3("Josh", 32, "Beijing"),
      Tuple3("peter", 35, "ShangHai"),
      Tuple3("li,nary", 26, "Wu,han")
    )) toDF("name", "age", "city")

    df.show()

    df.write
      .format("org.apache.hugegraph.spark.connector.DataSource")
      .option("host", "192.168.34.164")
      .option("port", "18080")
      .option("graph", "hugegraph")
      .option("data-type", "vertex")
      .option("label", "person")
      .option("id", "name")
      .option("batch-size", 2)
      .mode(SaveMode.Overwrite)
      .save()
  }

  private def insertVertices2(sparkSession: SparkSession): Unit = {
    val df = sparkSession.createDataFrame(Seq(
      Tuple4("lop", "java", 328L, "ISBN978-7-107-18618-5"),
      Tuple4("ripple", "python", 199L, "ISBN978-7-100-13678-5"),
    )) toDF("name", "lang", "price", "ISBN")

    df.show()

    df.write
      .format("org.apache.hugegraph.spark.connector.DataSource")
      .option("host", "192.168.34.164")
      .option("port", "18080")
      .option("graph", "hugegraph")
      .option("data-type", "vertex")
      .option("label", "software")
      .option("ignored-fields", "ISBN")
      .option("batch-size", 2)
      .mode(SaveMode.Overwrite)
      .save()
  }

  private def testInsertEdge1(sparkSession: SparkSession): Unit = {

    val df = sparkSession.createDataFrame(Seq(
      Tuple4("marko", "vadas", "20160110", 0.5),
      Tuple4("peter", "Josh", "20230801", 1.0),
      Tuple4("peter", "li,nary", "20130220", 2.0)
    )).toDF("source", "target", "date", "weight")

    df.show()

    df.write
      .format("org.apache.hugegraph.spark.connector.DataSource")
      .option("host", "192.168.34.164")
      .option("port", "18080")
      .option("graph", "hugegraph")
      .option("data-type", "edge")
      .option("label", "knows")
      .option("source-name", "source")
      .option("target-name", "target")
      .option("batch-size", 2)
      .mode(SaveMode.Overwrite)
      .save()
  }

  private def testInsertEdge2(sparkSession: SparkSession): Unit = {
    val df = sparkSession.createDataFrame(Seq(
      Tuple4("marko", "lop", "20171210", 0.5),
      Tuple4("Josh", "lop", "20091111", 0.4),
      Tuple4("peter", "ripple", "20171210", 1.0),
      Tuple4("vadas", "lop", "20171210", 0.2)
    )).toDF("source", "name", "date", "weight")

    df.show()

    df.write
      .format("org.apache.hugegraph.spark.connector.DataSource")
      .option("host", "192.168.34.164")
      .option("port", "18080")
      .option("graph", "hugegraph")
      .option("data-type", "edge")
      .option("label", "created")
      .option("source-name", "source")  // customize
      .option("target-name", "name")  // pk
      .option("batch-size", 2)
      .mode(SaveMode.Overwrite)
      .save()
  }
}
