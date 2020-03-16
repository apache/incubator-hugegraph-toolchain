// Define schema
schema.propertyKey("id").asText().ifNotExist().create();
schema.propertyKey("name").asText().ifNotExist().create();
schema.propertyKey("age").asInt().ifNotExist().create();
schema.propertyKey("city").asText().ifNotExist().create();

schema.vertexLabel("person").properties("id", "name", "age", "city")
      .useCustomizeStringId().ifNotExist().create();
