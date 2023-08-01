package org.apache.hugegraph.spark.connector.utils

import org.apache.hugegraph.spark.connector.builder.{EdgeBuilder, VertexBuilder}
import org.apache.hugegraph.spark.connector.client.HGLoadContext
import org.apache.hugegraph.structure.graph.{Edge, Vertex}
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.types.StructType
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters.{collectionAsScalaIterableConverter, seqAsJavaListConverter}

object HugeGraphBuildUtils {

  private val LOG = LoggerFactory.getLogger(this.getClass)

  // TODO refactor
  def buildVertices(row: InternalRow, schema: StructType, builder: VertexBuilder): List[Vertex] = {
    val fields = schema.names
    val dataTypes = schema.fields.map(field => field.dataType)
    val values = for {
      idx <- schema.fields.indices
    } yield {
      val value = row.get(idx, dataTypes(idx))
      if (value.getClass.getSimpleName.equalsIgnoreCase("UTF8String")) value.toString
      else value
    }
    LOG.info(s"Fields: ${fields.mkString(", ")}, values: ${values.mkString(", ")}")
    builder.build(fields, values.toArray).asScala.toList
  }

  def buildEdges(row: InternalRow, schema: StructType, builder: EdgeBuilder): List[Edge] = {
    val fields = schema.names
    val dataTypes = schema.fields.map(field => field.dataType)
    val values = for {
      idx <- schema.fields.indices
    } yield {
      val value = row.get(idx, dataTypes(idx))
      if (value.getClass.getSimpleName.equalsIgnoreCase("UTF8String")) value.toString
      else value
    }
    LOG.info(s"Fields: ${fields.mkString(", ")}, values: ${values.mkString(", ")}")
    builder.build(fields, values.toArray).asScala.toList
  }

  def saveVertices(context: HGLoadContext, vertices: List[Vertex]): List[Vertex] = {
    val successVertices = context.client().graph().addVertices(vertices.asJava).asScala.toList
    successVertices
  }

  def saveEdges(context: HGLoadContext, edges: List[Edge]): List[Edge] = {
    val successVertices = context.client().graph().addEdges(edges.asJava).asScala.toList
    successVertices
  }
}
