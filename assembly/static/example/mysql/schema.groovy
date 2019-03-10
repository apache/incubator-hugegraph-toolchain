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

schema.vertexLabel("person").useCustomizeNumberId().properties("name", "age", "city").nullableKeys("age", "city").ifNotExist().create();
schema.vertexLabel("software").useCustomizeNumberId().properties("name", "lang", "price").ifNotExist().create();

schema.edgeLabel("knows").sourceLabel("person").targetLabel("person").properties("date", "weight").ifNotExist().create();
schema.edgeLabel("created").sourceLabel("person").targetLabel("software").properties("date", "weight").ifNotExist().create();
schema.edgeLabel("use").sourceLabel("person").targetLabel("software").properties("feel", "time").nullableKeys("feel", "time").ifNotExist().create();
