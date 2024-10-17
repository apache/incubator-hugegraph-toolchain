package org.apache.hugegraph.serializer;

import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.serializer.direct.util.GraphSchema;

public abstract class AbstractGraphElementSerializer implements GraphElementSerializer {
    protected HugeClient client;
    protected GraphSchema graphSchema;

    public AbstractGraphElementSerializer(HugeClient client) {
        this.client = client;
        this.graphSchema = new GraphSchema(client);
    }

    public void close() {
        this.client.close();
    }

}

