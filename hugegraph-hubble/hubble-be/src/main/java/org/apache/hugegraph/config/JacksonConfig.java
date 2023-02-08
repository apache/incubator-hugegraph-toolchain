/*
 *
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

package org.apache.hugegraph.config;

import java.io.IOException;
import java.util.Map;

import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import org.apache.hugegraph.common.Response;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

@JsonComponent
public class JacksonConfig {

    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.createXmlMapper(false).build();

        SimpleModule module = new SimpleModule();
        // Add global serializer here
        mapper.registerModule(module);
        return mapper;
    }

    public static class ResponseSerailizer extends JsonSerializer<Response> {

        @Override
        public void serialize(Response response, JsonGenerator generator,
                              SerializerProvider provider) throws IOException {
            generator.writeStartObject();
            generator.writeNumberField("status", response.getStatus());
            generator.writeObjectField("data", response.getData());
            generator.writeObjectField("message", response.getMessage());
            Throwable cause = response.getCause();
            if (cause != null) {
                generator.writeStringField("cause", cause.toString());
            } else {
                generator.writeStringField("cause", null);
            }
            generator.writeEndObject();
        }
    }

    public static class VertexSerializer extends JsonSerializer<Vertex> {

        @Override
        public void serialize(Vertex vertex, JsonGenerator generator,
                              SerializerProvider provider) throws IOException {
            generator.writeStartObject();
            writeIdField("id", vertex.id(), generator);
            generator.writeStringField("label", vertex.label());
            writePropertiesField(vertex.properties(), generator, provider);
            generator.writeEndObject();
        }
    }

    public static class EdgeSerializer extends JsonSerializer<Edge> {

        @Override
        public void serialize(Edge edge, JsonGenerator generator,
                              SerializerProvider provider) throws IOException {
            generator.writeStartObject();
            if (edge.id() != null) {
                writeIdField("id", edge.id(), generator);
            }
            generator.writeStringField("label", edge.label());
            writeIdField("source", edge.sourceId(), generator);
            writeIdField("target", edge.targetId(), generator);
            writePropertiesField(edge.properties(), generator, provider);
            generator.writeEndObject();
        }
    }

    private static void writeIdField(String fieldName, Object id,
                                     JsonGenerator generator)
                                     throws IOException {
        // Serialize id to string
        generator.writeStringField(fieldName, id.toString());
    }

    private static void writePropertiesField(Map<String, Object> properties,
                                             JsonGenerator generator,
                                             SerializerProvider provider)
                                             throws IOException {
        // Start write properties
        generator.writeFieldName("properties");
        generator.writeStartObject();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            generator.writeFieldName(key);
            if (value != null) {
                if (value instanceof Long) {
                    // To avoid javascript loss of long precision
                    generator.writeString(String.valueOf(value));
                } else {
                    JsonSerializer<Object> serializer;
                    serializer = provider.findValueSerializer(value.getClass());
                    serializer.serialize(value, generator, provider);
                }
            } else {
                generator.writeNull();
            }
        }
        // End wirte properties
        generator.writeEndObject();
    }
}
