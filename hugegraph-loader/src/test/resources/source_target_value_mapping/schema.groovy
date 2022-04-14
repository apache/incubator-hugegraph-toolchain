// Define schema
schema.propertyKey("name").asText().ifNotExist().create();
schema.propertyKey("age").asInt().ifNotExist().create();
schema.propertyKey("city").asText().ifNotExist().create();
schema.propertyKey("weight").asDouble().ifNotExist().create();
schema.propertyKey("lang").asText().ifNotExist().create();
schema.propertyKey("price").asDouble().ifNotExist().create();

schema.vertexLabel("person").useCustomizeStringId().properties("name", "age", "city").ifNotExist().create();
schema.vertexLabel("software").useCustomizeStringId().properties("name", "lang", "price").ifNotExist().create();

schema.edgeLabel("created").sourceLabel("person").targetLabel("software").properties("weight").ifNotExist().create();
