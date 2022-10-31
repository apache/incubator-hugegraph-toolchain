package com.baidu.hugegraph.loader.direct.loader;

import com.baidu.hugegraph.loader.builder.EdgeBuilder;
import com.baidu.hugegraph.loader.builder.ElementBuilder;
import com.baidu.hugegraph.loader.builder.VertexBuilder;
import com.baidu.hugegraph.loader.executor.LoadContext;
import com.baidu.hugegraph.loader.executor.LoadOptions;
import com.baidu.hugegraph.loader.mapping.EdgeMapping;
import com.baidu.hugegraph.loader.mapping.InputStruct;
import com.baidu.hugegraph.loader.mapping.VertexMapping;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * 1.build
 * 2.ser
 * 3.generateFile
 * 4.load
 */
public abstract class DirectLoader<T,R> implements Serializable {
    LoadOptions loadOptions ;
    InputStruct struct;


    public DirectLoader(LoadOptions loadOptions,
                        InputStruct struct) {
        this.loadOptions = loadOptions;
        this.struct=struct;
    }




    public void bulkload(Dataset<Row> ds){
        JavaPairRDD<T, R> javaPairRDD = buildVertexAndEdge(ds);
        String path = generateFiles(javaPairRDD);
        loadFiles(path);
    };


    protected List<ElementBuilder> getElementBuilders(){
        LoadContext context = new LoadContext(loadOptions);
        List<ElementBuilder> buildersForGraphElement = new LinkedList<>();
        for (VertexMapping vertexMapping : struct.vertices()) {
            buildersForGraphElement.add(
                    new VertexBuilder(context, struct, vertexMapping)
            );
        }
        for (EdgeMapping edgeMapping : struct.edges()) {
            buildersForGraphElement.add(new EdgeBuilder(context, struct, edgeMapping));
        }
        context.close();
        return buildersForGraphElement;
    }


    abstract JavaPairRDD<T, R> buildVertexAndEdge(Dataset<Row> ds);



    abstract String generateFiles(JavaPairRDD<T,  R> buildAndSerRdd);

    abstract void loadFiles(String path);
}
