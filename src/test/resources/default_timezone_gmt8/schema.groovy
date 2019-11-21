// Define schema
schema.propertyKey("name").asText().ifNotExist().create();
schema.propertyKey("birth").asDate().ifNotExist().create();
schema.propertyKey("city").asText().ifNotExist().create();

schema.vertexLabel("person").properties("name", "birth", "city").primaryKeys("name").ifNotExist().create();
