// Define schema
schema.propertyKey("name").asText().ifNotExist().create();
schema.propertyKey("age").asInt().ifNotExist().create();
schema.propertyKey("city").asText().ifNotExist().create();
schema.propertyKey("weight").asDouble().ifNotExist().create();
schema.propertyKey("lang").asText().ifNotExist().create();
schema.propertyKey("date").asDate().ifNotExist().create();
schema.propertyKey("price").asDouble().ifNotExist().create();

schema.vertexLabel("person")
      .properties("name", "age", "city")
      .primaryKeys("name")
      .nullableKeys("age", "city")
      .ifNotExist()
      .create();

schema.edgeLabel("knows")
      .sourceLabel("person")
      .targetLabel("person")
      .properties("date", "weight")
      .ifNotExist()
      .create();
