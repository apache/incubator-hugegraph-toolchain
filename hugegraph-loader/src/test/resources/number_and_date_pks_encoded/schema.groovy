// Define schema
schema.propertyKey("id").asLong().ifNotExist().create();
schema.propertyKey("name").asText().ifNotExist().create();
schema.propertyKey("age").asInt().ifNotExist().create();
schema.propertyKey("city").asText().ifNotExist().create();
schema.propertyKey("weight").asDouble().ifNotExist().create();
schema.propertyKey("lang").asText().ifNotExist().create();
schema.propertyKey("date").asDate().ifNotExist().create();
schema.propertyKey("price").asDouble().ifNotExist().create();
schema.propertyKey("feel").asText().valueList().ifNotExist().create();
schema.propertyKey("time").asText().valueSet().ifNotExist().create();

schema.vertexLabel("person").properties("id", "name", "age", "city").primaryKeys("id").ifNotExist().create();
schema.vertexLabel("software").properties("date", "name", "lang", "price").primaryKeys("date").ifNotExist().create();

schema.edgeLabel("created").sourceLabel("person").targetLabel("software").properties("date", "weight").ifNotExist().create();
