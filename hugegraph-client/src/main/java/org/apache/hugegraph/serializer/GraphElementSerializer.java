package org.apache.hugegraph.serializer;

import org.apache.hugegraph.serializer.direct.struct.Directions;
import org.apache.hugegraph.structure.GraphElement;
import scala.Tuple2;

public interface GraphElementSerializer {

    Tuple2<byte[], Integer> getKeyBytes(GraphElement e, Directions direction);
    byte[] getValueBytes(GraphElement e);

}
