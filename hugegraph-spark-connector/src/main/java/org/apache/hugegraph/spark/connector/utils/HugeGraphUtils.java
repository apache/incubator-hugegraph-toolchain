package org.apache.hugegraph.spark.connector.utils;

import java.util.List;

import org.apache.hugegraph.spark.connector.mapping.EdgeMapping;
import org.apache.hugegraph.spark.connector.mapping.VertexMapping;
import org.apache.hugegraph.spark.connector.options.HGOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HugeGraphUtils {

    private static final Logger LOG = LoggerFactory.getLogger(HugeGraphUtils.class);

    public static VertexMapping vertexMappingFromConf(HGOptions hgOptions) {
        String idField = hgOptions.idField();
        VertexMapping vertexMapping = new VertexMapping(idField);

        String label = hgOptions.label();
        vertexMapping.label(label);
        vertexMapping.batchSize(hgOptions.batchSize());
        vertexMapping.selectedFields(hgOptions.selectedFields());
        vertexMapping.ignoredFields(hgOptions.ignoredFields());
        vertexMapping.check();

        // TODO mappingFields, mappingValues, nullValues, updateStrategies
        LOG.info("Update VertexMapping: {}", vertexMapping);
        return vertexMapping;
    }

    public static EdgeMapping edgeMappingFromConf(HGOptions hgOptions) {
        List<String> sourceNames = hgOptions.sourceName();
        List<String> targetNames = hgOptions.targetName();
        EdgeMapping edgeMapping = new EdgeMapping(sourceNames, targetNames);

        String label = hgOptions.label();
        edgeMapping.label(label);
        edgeMapping.batchSize(hgOptions.batchSize());
        edgeMapping.selectedFields(hgOptions.selectedFields());
        edgeMapping.ignoredFields(hgOptions.ignoredFields());
        edgeMapping.check();

        // TODO mappingFields, mappingValues, nullValues, updateStrategies
        LOG.info("Update EdgeMapping: {}", edgeMapping);
        return edgeMapping;
    }

    //public static List<Vertex> buildVertices(InternalRow row, StructType schema,
    //                                         VertexBuilder builder) {
    //    int len = schema.length();
    //    String[] fields = schema.names();
    //    Object[] values = new String[len];
    //    List<DataType> dataTypes = Arrays.stream(schema.fields()).map(StructField::dataType)
    //                                   .collect(Collectors.toList());
    //    for (int i = 0; i < len; i++) {
    //        values[i] = row.get(i, dataTypes.get(i));
    //    }
    //    LOG.info("Fields: {}, values: {}", Arrays.toString(fields), Arrays.toString(values));
    //    return builder.build(fields, values);
    //}
}
