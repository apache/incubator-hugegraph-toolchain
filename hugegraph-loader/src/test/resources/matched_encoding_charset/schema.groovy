// Define schema
schema.propertyKey("name").asText().ifNotExist().create();
schema.propertyKey("lang").asText().ifNotExist().create();
schema.propertyKey("price").asDouble().ifNotExist().create();

schema.vertexLabel("software").properties("name", "lang", "price").primaryKeys("name").ifNotExist().create();
