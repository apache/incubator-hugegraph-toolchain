// Define schema
schema.propertyKey("name").asText().ifNotExist().create();
schema.propertyKey("age").asInt().ifNotExist().create();
schema.propertyKey("city").asText().ifNotExist().create();
schema.propertyKey("weight").asDouble().ifNotExist().create();
schema.propertyKey("lang").asText().ifNotExist().create();
schema.propertyKey("date").asText().ifNotExist().create();
schema.propertyKey("price").asDouble().ifNotExist().create();
schema.propertyKey("feel").asText().valueList().ifNotExist().create();
schema.propertyKey("time").asText().valueSet().ifNotExist().create();

schema.vertexLabel("person").properties("name", "age", "city").useCustomizeStringId().ifNotExist().create();
schema.vertexLabel("software").properties("name", "lang", "price").useCustomizeStringId().ifNotExist().create();

schema.edgeLabel("created").sourceLabel("person").targetLabel("software").properties("date", "weight").ifNotExist().create();
