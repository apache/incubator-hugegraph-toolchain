// Define schema
schema.propertyKey("name").asText().ifNotExist().create();
schema.propertyKey("age").asInt().ifNotExist().create();
schema.propertyKey("city").asText().ifNotExist().create();
schema.propertyKey("weight").asLong().ifNotExist().create();
schema.propertyKey("date").asText().ifNotExist().create();

schema.vertexLabel("person").properties("name", "age", "city", "weight").primaryKeys("name").ifNotExist().create();

schema.edgeLabel("knows").sourceLabel("person").targetLabel("person").properties("date", "weight").ifNotExist().create();
