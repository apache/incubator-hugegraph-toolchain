// Define schema
schema.propertyKey("name").asText().ifNotExist().create();
schema.propertyKey("age").asInt().ifNotExist().create();
schema.propertyKey("city").asText().ifNotExist().create();
schema.propertyKey("no").asUuid().ifNotExist().create();

schema.vertexLabel("person").properties("name", "age", "city", "no").primaryKeys("name").ifNotExist().create();
