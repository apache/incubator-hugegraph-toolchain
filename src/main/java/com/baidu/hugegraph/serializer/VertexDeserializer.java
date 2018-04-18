package com.baidu.hugegraph.serializer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.baidu.hugegraph.exception.InvalidResponseException;
import com.baidu.hugegraph.structure.graph.Vertex;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class VertexDeserializer extends JsonDeserializer<Vertex> {

    private final ObjectMapper mapper;

    public VertexDeserializer() {
        this.mapper = new ObjectMapper();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Vertex deserialize(JsonParser parser, DeserializationContext ctxt)
                              throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);

        JsonNode idNode = node.get("id");
        if (idNode == null) {
            throw InvalidResponseException.expectField("id", node);
        }
        Object id = mapper.convertValue(idNode, Object.class);

        JsonNode labelNode = node.get("label");
        if (labelNode == null ||
            labelNode.getNodeType() != JsonNodeType.STRING) {
            throw InvalidResponseException.expectField("label", node);
        }
        String label = labelNode.asText();

        JsonNode propNode = node.get("properties");
        if (propNode == null ||
            propNode.getNodeType() != JsonNodeType.OBJECT) {
            throw InvalidResponseException.expectField("properties", node);
        }

        Vertex vertex = new Vertex(label);
        vertex.id(id);
        Map<String, Object> props = mapper.convertValue(propNode, Map.class);
        this.pruneProperties(vertex, props);
        return vertex;
    }

    @SuppressWarnings("unchecked")
    private void pruneProperties(Vertex vertex, Map<String, Object> props) {
        Map<String, Object> vertexProps = vertex.properties();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (!(value instanceof List)) {
                throw new InvalidResponseException(
                          "The value of key '%s' should be list type, " +
                          "but it's actually a '%s'", key, value.getClass());
            }
            List<Object> elems = ((List<Object>) value);
            if (elems.size() != 1 || !(elems.get(0) instanceof Map)) {
                throw new InvalidResponseException(
                          "There should be only one object element in the " +
                          "value of key '%s', actual are %s", key, elems);
            }
            Map<String, ?> elem = (Map<String, ?>) elems.get(0);
            if (!elem.containsKey("value")) {
                throw InvalidResponseException.expectField("value", elem);
            }
            vertexProps.put(entry.getKey(), elem.get("value"));
        }
    }
}
