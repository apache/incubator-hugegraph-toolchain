// Define schema
schema.propertyKey("name").asText().ifNotExist().create();
schema.propertyKey("age").asInt().ifNotExist().create();
schema.propertyKey("city").asText().ifNotExist().create();
schema.propertyKey("isMale").asBoolean().ifNotExist().create();

schema.vertexLabel("person").properties("name", "age", "city", "isMale").primaryKeys("name").ifNotExist().create();
