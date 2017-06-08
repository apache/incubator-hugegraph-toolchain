package com.baidu.hugegraph.driver;

import org.junit.Assert;
import org.junit.Test;

import com.baidu.hugegraph.exception.ClientException;
import com.baidu.hugegraph.structure.constant.DataType;
import com.baidu.hugegraph.structure.constant.EdgeLink;
import com.baidu.hugegraph.structure.constant.Frequency;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.structure.constant.IndexType;
import com.baidu.hugegraph.structure.constant.T;
import com.baidu.hugegraph.structure.schema.EdgeLabel;
import com.baidu.hugegraph.structure.schema.IndexLabel;
import com.baidu.hugegraph.structure.schema.PropertyKey;
import com.baidu.hugegraph.structure.schema.VertexLabel;

public class SchemaTest extends BaseTest{

    // propertykey tests
    @Test
    public void testAddPropertyKeyWithoutDataType() {
        SchemaManager schema = client().schema();
        PropertyKey id = schema.makePropertyKey("id").create();
        Assert.assertNotNull(id);
        Assert.assertEquals(DataType.TEXT, id.dataType());
    }

    @Test
    public void testAddPropertyKey() {
        SchemaManager schema = client().schema();
        schema.makePropertyKey("name").asText().create();

        Assert.assertNotNull(schema.getPropertyKey("name"));
        Assert.assertEquals("name", schema.getPropertyKey("name").name());
        Assert.assertEquals(DataType.TEXT,
                schema.getPropertyKey("name").dataType());
    }

    // vertexlabel tests
    @Test
    public void testAddVertexLabel() {
        initProperties();
        SchemaManager schema = client().schema();

        VertexLabel person = schema.makeVertexLabel("person")
                .properties("name", "age", "city")
                .primaryKeys("name")
                .create();

        Assert.assertNotNull(person);
        Assert.assertEquals("person", person.name());
        Assert.assertEquals(3, person.properties().size());
        Assert.assertTrue(person.properties().contains("name"));
        Assert.assertTrue(person.properties().contains("age"));
        Assert.assertTrue(person.properties().contains("city"));
        Assert.assertEquals(1, person.primaryKeys().size());
        Assert.assertTrue(person.primaryKeys().contains("name"));
    }

    @Test
    public void testAddVertexLabelWith2PrimaryKey() {
        initProperties();
        SchemaManager schema = client().schema();

        VertexLabel person = schema.makeVertexLabel("person")
                .properties("name", "age", "city")
                .primaryKeys("name", "age")
                .create();

        Assert.assertNotNull(person);
        Assert.assertEquals("person", person.name());
        Assert.assertEquals(3, person.properties().size());
        Assert.assertTrue(person.properties().contains("name"));
        Assert.assertTrue(person.properties().contains("age"));
        Assert.assertTrue(person.properties().contains("city"));
        Assert.assertEquals(2, person.primaryKeys().size());
        Assert.assertTrue(person.primaryKeys().contains("name"));
        Assert.assertTrue(person.primaryKeys().contains("age"));
    }

    @Test
    public void testAddVertexLabelWithoutPrimaryKey() {
        initProperties();
        SchemaManager schema = client().schema();

        Utils.assertThrows(ClientException.class, () -> {
            schema.makeVertexLabel("person")
                    .properties("name", "age", "city")
                    .create();
        });
    }

    @Test
    public void testAddVertexLabelWithoutPropertyKey() {
        initProperties();
        SchemaManager schema = client().schema();

        Utils.assertThrows(ClientException.class, () -> {
            schema.makeVertexLabel("person").create();
        });
    }

    @Test
    public void testAddVertexLabelWithNotExistProperty() {
        initProperties();
        SchemaManager schema = client().schema();

        Utils.assertThrows(ClientException.class, () -> {
            schema.makeVertexLabel("person").properties("sex").create();
        });
    }

    @Test
    public void testAddVertexLabelNewVertexWithUndefinedProperty() {
        initProperties();
        SchemaManager schema = client().schema();

        schema.makeVertexLabel("person")
                .properties("name", "age", "city")
                .primaryKeys("name")
                .create();

        Utils.assertThrows(ClientException.class, () -> {
            client().graph().addVertex(T.label, "person", "name", "Baby",
                    "city", "Hongkong", "age", 3, "sex", "male");
        });
    }

    @Test
    public void testAddVertexLabelNewVertexWithPropertyAbsent() {
        initProperties();
        SchemaManager schema = client().schema();
        VertexLabel person = schema.makeVertexLabel("person")
                .properties("name", "age", "city")
                .primaryKeys("name")
                .create();

        client().graph().addVertex(T.label, "person", "name", "Baby",
                "city", "Hongkong");

        Assert.assertNotNull(person);
        Assert.assertEquals("person", person.name());
        Assert.assertEquals(3, person.properties().size());
        Assert.assertTrue(person.properties().contains("name"));
        Assert.assertTrue(person.properties().contains("age"));
        Assert.assertTrue(person.properties().contains("city"));
        Assert.assertEquals(1, person.primaryKeys().size());
        Assert.assertTrue(person.primaryKeys().contains("name"));
    }

    @Test
    public void testAddVertexLabelNewVertexWithUnmatchPropertyType() {
        initProperties();
        SchemaManager schema = client().schema();

        schema.makeVertexLabel("person")
                .properties("name", "age", "city")
                .primaryKeys("name")
                .create();

        Utils.assertThrows(ClientException.class, () -> {
            client().graph().addVertex(T.label, "person", "name", "Baby",
                    "city", 2, "age", 3);
        });

    }

    // edgelabel tests
    @Test
    public void testAddEdgeLabel() {
        initProperties();
        SchemaManager schema = client().schema();
        schema.makeVertexLabel("person")
                .properties("name", "age", "city")
                .primaryKeys("name")
                .create();
        schema.makeVertexLabel("author").properties("id", "name")
                .primaryKeys("id").create();
        schema.makeVertexLabel("book").properties("id", "name")
                .primaryKeys("id").create();
        EdgeLabel look = schema.makeEdgeLabel("look").multiTimes()
                .properties("time")
                .link("author", "book")
                .link("person", "book")
                .sortKeys("time")
                .create();

        Assert.assertNotNull(look);
        Assert.assertEquals("look", look.name());
        Assert.assertEquals(2, look.links().size());
        Assert.assertTrue(look.links()
                .contains(new EdgeLink("author", "book")));
        Assert.assertTrue(look.links()
                .contains(new EdgeLink("person", "book")));
        Assert.assertEquals(1, look.properties().size());
        Assert.assertTrue(look.properties().contains("time"));
        Assert.assertEquals(1, look.sortKeys().size());
        Assert.assertTrue(look.sortKeys().contains("time"));
        Assert.assertEquals(Frequency.MULTIPLE, look.frequency());
    }

    @Test
    public void testAddEdgeLabelWithoutFrequency() {
        initProperties();
        SchemaManager schema = client().schema();
        schema.makeVertexLabel("person")
                .properties("name", "age", "city")
                .primaryKeys("name")
                .create();
        schema.makeVertexLabel("author").properties("id", "name")
                .primaryKeys("id").create();
        schema.makeVertexLabel("book").properties("id", "name")
                .primaryKeys("id").create();
        EdgeLabel look = schema.makeEdgeLabel("look").properties("time")
                .link("author", "book")
                .link("person", "book")
                .create();

        Assert.assertNotNull(look);
        Assert.assertEquals("look", look.name());
        Assert.assertEquals(2, look.links().size());
        Assert.assertTrue(look.links()
                .contains(new EdgeLink("author", "book")));
        Assert.assertTrue(look.links()
                .contains(new EdgeLink("person", "book")));
        Assert.assertEquals(1, look.properties().size());
        Assert.assertTrue(look.properties().contains("time"));
        Assert.assertEquals(0, look.sortKeys().size());
        Assert.assertEquals(Frequency.SINGLE, look.frequency());
    }

    @Test
    public void testAddEdgeLabelWithoutProperty() {
        initProperties();
        SchemaManager schema = client().schema();
        schema.makeVertexLabel("person")
                .properties("name", "age", "city")
                .primaryKeys("name")
                .create();
        schema.makeVertexLabel("author").properties("id", "name")
                .primaryKeys("id").create();
        schema.makeVertexLabel("book").properties("id", "name")
                .primaryKeys("id").create();
        EdgeLabel look = schema.makeEdgeLabel("look").singleTime()
                .link("author", "book")
                .link("person", "book")
                .create();

        Assert.assertNotNull(look);
        Assert.assertEquals("look", look.name());
        Assert.assertEquals(2, look.links().size());
        Assert.assertTrue(look.links()
                .contains(new EdgeLink("author", "book")));
        Assert.assertTrue(look.links()
                .contains(new EdgeLink("person", "book")));
        Assert.assertEquals(0, look.properties().size());
        Assert.assertEquals(0, look.sortKeys().size());
        Assert.assertEquals(Frequency.SINGLE, look.frequency());
    }

    @Test
    public void testAddEdgeLabelWithoutLink() {
        initProperties();
        SchemaManager schema = client().schema();
        schema.makeVertexLabel("person")
                .properties("name", "age", "city")
                .primaryKeys("name")
                .create();
        schema.makeVertexLabel("author").properties("id", "name")
                .primaryKeys("id").create();

        Utils.assertThrows(ClientException.class, () -> {
            EdgeLabel look = schema.makeEdgeLabel("look").multiTimes()
                    .properties("time")
                    .sortKeys("time")
                    .create();
        });
    }

    @Test
    public void testAddEdgeLabelWithNotExistProperty() {
        initProperties();
        SchemaManager schema = client().schema();
        schema.makeVertexLabel("person")
                .properties("name", "age", "city")
                .primaryKeys("name")
                .create();
        schema.makeVertexLabel("author").properties("id", "name")
                .primaryKeys("id").create();

        Utils.assertThrows(ClientException.class, () -> {
            schema.makeEdgeLabel("look").properties("date-time")
                    .link("author", "book")
                    .link("person", "book")
                    .create();
        });
    }

    @Test
    public void testAddEdgeLabelWithNotExistVertexLabel() {
        initProperties();
        SchemaManager schema = client().schema();
        schema.makeVertexLabel("person")
                .properties("name", "age", "city")
                .primaryKeys("name")
                .create();
        schema.makeVertexLabel("author").properties("id", "name")
                .primaryKeys("id").create();

        Utils.assertThrows(ClientException.class, () -> {
            schema.makeEdgeLabel("look").multiTimes().properties("time")
                    .link("reviewer", "book")
                    .link("person", "book")
                    .sortKeys("time")
                    .create();
        });
    }

    @Test
    public void testAddEdgeLabelMultipleWithoutSortKey() {
        initProperties();
        SchemaManager schema = client().schema();
        schema.makeVertexLabel("person")
                .properties("name", "age", "city")
                .primaryKeys("name")
                .create();
        schema.makeVertexLabel("author").properties("id", "name")
                .primaryKeys("id").create();

        Utils.assertThrows(ClientException.class, () -> {
            schema.makeEdgeLabel("look").multiTimes().properties("date")
                    .link("author", "book")
                    .link("person", "book")
                    .create();
        });
    }

    @Test
    public void testAddEdgeLabelSortKeyNotInProperty() {
        initProperties();
        SchemaManager schema = client().schema();
        schema.makeVertexLabel("person")
                .properties("name", "age", "city")
                .primaryKeys("name")
                .create();
        schema.makeVertexLabel("author").properties("id", "name")
                .primaryKeys("id").create();

        Utils.assertThrows(ClientException.class, () -> {
            schema.makeEdgeLabel("look").multiTimes().properties("date")
                    .link("author", "book")
                    .link("person", "book")
                    .sortKeys("time")
                    .create();
        });
    }

    // indexlabel tests
    @Test
    public void testAddIndexLabelOfVertex() {
        initProperties();
        SchemaManager schema = client().schema();
        VertexLabel person = schema.makeVertexLabel("person")
                .properties("name", "age", "city")
                .primaryKeys("name")
                .create();
        IndexLabel personByCity = schema.makeIndexLabel("personByCity")
                .on(person).secondary().by("city").create();
        IndexLabel personByAge = schema.makeIndexLabel("personByAge")
                .on(person).search().by("age").create();

        Assert.assertNotNull(personByCity);
        Assert.assertNotNull(personByAge);
        Assert.assertEquals(2, person.indexNames().size());
        Assert.assertTrue(person.indexNames().contains("personByCity"));
        Assert.assertTrue(person.indexNames().contains("personByAge"));
        Assert.assertEquals(HugeType.VERTEX_LABEL, personByCity.baseType());
        Assert.assertEquals(HugeType.VERTEX_LABEL, personByAge.baseType());
        Assert.assertEquals("person", personByCity.baseValue());
        Assert.assertEquals("person", personByAge.baseValue());
        Assert.assertEquals(IndexType.SECONDARY, personByCity.indexType());
        Assert.assertEquals(IndexType.SEARCH, personByAge.indexType());
    }

    @Test
    public void testAddIndexLabelOfEdge() {
        initProperties();
        SchemaManager schema = client().schema();

        schema.makeVertexLabel("author").properties("id", "name")
                .primaryKeys("id").create();
        schema.makeVertexLabel("book").properties("name")
                .primaryKeys("name").create();
        EdgeLabel authored = schema.makeEdgeLabel("authored").singleTime()
                .link("author", "book")
                .properties("contribution", "time")
                .create();

        IndexLabel authoredByContri = schema.makeIndexLabel("authoredByContri")
                .on(authored)
                .secondary()
                .by("time")
                .create();

        Assert.assertNotNull(authoredByContri);
        Assert.assertEquals(1, authored.indexNames().size());
        Assert.assertTrue(authored.indexNames().contains("authoredByContri"));
        Assert.assertEquals(HugeType.EDGE_LABEL, authoredByContri.baseType());
        Assert.assertEquals("authored", authoredByContri.baseValue());
        Assert.assertEquals(IndexType.SECONDARY, authoredByContri.indexType());
    }

    // utils
    public void initProperties() {
        SchemaManager schema = client().schema();
        schema.makePropertyKey("id").asInt().create();
        schema.makePropertyKey("name").asText().create();
        schema.makePropertyKey("age").asInt().valueSingle().create();
        schema.makePropertyKey("city").asText().create();
        schema.makePropertyKey("time").asText().create();
        schema.makePropertyKey("contribution").asText().valueSet().create();
    }
}
