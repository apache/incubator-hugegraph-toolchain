// Define schema
schema.propertyKey("name").asText().ifNotExist().create();
schema.propertyKey("age").asInt().ifNotExist().create();
schema.propertyKey("city").asText().ifNotExist().create();

schema.vertexLabel("person").useCustomizeNumberId().properties("name", "age", "city").nullableKeys("age", "city").ifNotExist().create();
