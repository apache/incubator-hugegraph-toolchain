// Define schema
schema.propertyKey("name").asText().ifNotExist().create();
schema.propertyKey("p_boolean").asBoolean().ifNotExist().create();
schema.propertyKey("p_byte").asByte().ifNotExist().create();
schema.propertyKey("p_int").asInt().ifNotExist().create();
schema.propertyKey("p_long").asLong().ifNotExist().create();
schema.propertyKey("p_float").asFloat().ifNotExist().create();
schema.propertyKey("p_double").asDouble().ifNotExist().create();
schema.propertyKey("p_string").asText().ifNotExist().create();
schema.propertyKey("p_date").asDate().ifNotExist().create();

schema.vertexLabel("person")
      .properties("name", "p_boolean", "p_byte", "p_int", "p_long", "p_float", "p_double", "p_string", "p_date")
      .primaryKeys("name")
      .ifNotExist().create();
