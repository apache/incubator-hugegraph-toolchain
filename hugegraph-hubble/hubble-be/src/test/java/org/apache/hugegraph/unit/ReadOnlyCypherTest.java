/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.unit;

import org.apache.hugegraph.exception.ExternalException;
import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.util.ReadOnlyCypher;
import org.junit.Test;

public class ReadOnlyCypherTest {

    @Test
    public void addsPreviewDefaultOnlyAfterTheFinalReturn() {
        this.assertPreview("MATCH (n) RETURN n", "MATCH (n) RETURN n LIMIT 300", true);
        this.assertPreview("MATCH (n) WITH n LIMIT 7 RETURN n",
                           "MATCH (n) WITH n LIMIT 7 RETURN n LIMIT 300", true);
        this.assertPreview("MATCH (n) RETURN n;  ", "MATCH (n) RETURN n LIMIT 300;  ", true);
        this.assertPreview("UNWIND [1, 2, 3] AS n RETURN n",
                           "UNWIND [1, 2, 3] AS n RETURN n LIMIT 300", true);
        this.assertPreview("MATCH (n) RETURN n.count, n.collect, n.limit",
                           "MATCH (n) RETURN n.count, n.collect, n.limit LIMIT 300", true);
    }

    @Test
    public void capsOnlyTheFinalLiteralReturnLimit() {
        this.assertPreview("MATCH (n) WITH n LIMIT 20000 RETURN n LIMIT 10000",
                           "MATCH (n) WITH n LIMIT 20000 RETURN n LIMIT 1001", false);
        this.assertPreview("MATCH (n) WITH n LIMIT 20000 RETURN n LIMIT 7",
                           "MATCH (n) WITH n LIMIT 20000 RETURN n LIMIT 7", false);
        this.assertPreview("MATCH (n)\nRETURN n LIMIT 10000;  ",
                           "MATCH (n)\nRETURN n LIMIT 1001;  ", false);
        this.assertPreview("RETURN 'LIMIT 10000' AS text LIMIT 10000",
                           "RETURN 'LIMIT 10000' AS text LIMIT 1001", false);
    }

    @Test
    public void limitsAggregateOutputWithoutChangingItsInput() {
        this.assertPreview("MATCH (n) RETURN count(n) AS total",
                           "MATCH (n) RETURN count(n) AS total LIMIT 300", true);
        this.assertPreview("MATCH (n) RETURN n.city, count(*) AS total ORDER BY total DESC",
                           "MATCH (n) RETURN n.city, count(*) AS total ORDER BY total DESC LIMIT 300", true);
        this.assertPreview("MATCH (n) RETURN n.city, count(*) LIMIT 10000",
                           "MATCH (n) RETURN n.city, count(*) LIMIT 1001", false);
        this.assertPreview("MATCH (n) WITH count(n) AS total RETURN total",
                           "MATCH (n) WITH count(n) AS total RETURN total LIMIT 300", true);
    }

    @Test
    public void requiresBoundedInputBeforeCollectingPreviewResults() {
        String[] queries = {
            "MATCH (n) RETURN collect(n)",
            "MATCH (n) RETURN collect(n) LIMIT 1",
            "MATCH (n) WITH collect(n) AS nodes RETURN nodes",
            "MATCH (n) WITH n LIMIT 1001 RETURN collect(n)",
            "MATCH (n) WITH n LIMIT 1000 MATCH (n)-->(m) RETURN collect(m)",
            "MATCH (n) WITH n LIMIT 1000 OPTIONAL MATCH (n)-->(m) RETURN collect(m)",
            "MATCH (n) WITH n LIMIT 1000 UNWIND n.items AS item RETURN collect(item)"
        };
        for (String query : queries) {
            ReadOnlyCypher.validate(query);
            ExternalException error = (ExternalException) Assert.assertThrows(
                                      ExternalException.class, () -> ReadOnlyCypher.preview(query, 300, 1001));
            Assert.assertEquals("gremlin.preview.collection_requires_async", error.getMessage());
            Assert.assertEquals(1000, error.args()[0]);
        }
    }

    @Test
    public void acceptsCollectAfterAnUpstreamWithLimitAndPreservesNonExpandingBounds() {
        this.assertPreview("MATCH (n) WITH n LIMIT 1000 RETURN collect(n)",
                           "MATCH (n) WITH n LIMIT 1000 RETURN collect(n) LIMIT 300", true);
        this.assertPreview("MATCH (n) WITH n LIMIT 1000 WITH collect(n) AS nodes RETURN nodes",
                           "MATCH (n) WITH n LIMIT 1000 WITH collect(n) AS nodes RETURN nodes LIMIT 300", true);
        this.assertPreview("MATCH (n) WITH n LIMIT 1000 WITH n.name AS name RETURN collect(name) LIMIT 1",
                           "MATCH (n) WITH n LIMIT 1000 WITH n.name AS name RETURN collect(name) LIMIT 1", false);
        this.assertPreview("MATCH (n) WITH n LIMIT 1000 WITH n LIMIT 2000 RETURN collect(n)",
                           "MATCH (n) WITH n LIMIT 1000 WITH n LIMIT 2000 RETURN collect(n) LIMIT 300", true);
    }

    @Test
    public void requiresAsyncForLargeOrUnprovenRangeValues() {
        String[] queries = {
            "RETURN range(1, 1000000000)",
            "RETURN range(1, 1000000000) LIMIT 1",
            "UNWIND range(1, 1000000000) AS item RETURN item",
            "MATCH (n) WITH n LIMIT 10 RETURN range(1, 1000000000)",
            "MATCH (n) RETURN range(1, n.count)",
            "RETURN range(1, 2 + 3)"
        };
        for (String query : queries) {
            ReadOnlyCypher.validate(query);
            ExternalException error = (ExternalException) Assert.assertThrows(
                                      ExternalException.class, () -> ReadOnlyCypher.preview(query, 300, 1001));
            Assert.assertEquals("gremlin.preview.collection_requires_async", error.getMessage());
            Assert.assertEquals(1000, error.args()[0]);
        }
    }

    @Test
    public void acceptsProvablyBoundedRangesIncludingDescendingSteps() {
        this.assertPreview("RETURN range(1, 10)", "RETURN range(1, 10) LIMIT 300", true);
        this.assertPreview("RETURN range(1, 1000)", "RETURN range(1, 1000) LIMIT 300", true);
        this.assertPreview("RETURN range(10, 1, -1)", "RETURN range(10, 1, -1) LIMIT 300", true);
    }

    @Test
    public void preservesTotalInputBoundsThroughLiteralUnwind() {
        String[] queries = {
            "UNWIND range(1, 10) AS n RETURN collect(n)",
            "UNWIND [1, 2, 3] AS n RETURN collect(n)",
            "UNWIND range(1, 10) AS n UNWIND range(1, 100) AS m RETURN collect(m)",
            "MATCH (n) WITH n LIMIT 5 UNWIND range(1, 10) AS m RETURN collect(m)",
            "MATCH (n) WITH n LIMIT 500 UNWIND [1, 2] AS m RETURN collect(m)",
            "MATCH (n) WITH n LIMIT 5 WITH n LIMIT 2000 UNWIND range(1, 10) AS m RETURN collect(m)"
        };
        for (String query : queries) {
            ReadOnlyCypher.validate(query);
            this.assertPreview(query, query + " LIMIT 300", true);
        }
    }

    @Test
    public void requiresAsyncWhenLiteralUnwindExpandsUnknownOrExcessiveInput() {
        String[] queries = {
            "MATCH (n) UNWIND range(1, 10) AS m RETURN collect(m)",
            "MATCH (n) UNWIND [1, 2] AS m RETURN collect(m)",
            "UNWIND range(1, 10) AS n UNWIND range(1, 101) AS m RETURN collect(m)",
            "MATCH (n) WITH n LIMIT 1000 UNWIND range(1, 10) AS m RETURN collect(m)",
            "MATCH (n) WITH n LIMIT 501 UNWIND [1, 2] AS m RETURN collect(m)"
        };
        for (String query : queries) {
            ReadOnlyCypher.validate(query);
            ExternalException error = (ExternalException) Assert.assertThrows(
                                      ExternalException.class, () -> ReadOnlyCypher.preview(query, 300, 1001));
            Assert.assertEquals("gremlin.preview.collection_requires_async", error.getMessage());
            Assert.assertEquals(1000, error.args()[0]);
        }
    }

    @Test
    public void preservesUnsupportedManualQueriesAndLimitExpressions() {
        String[] queries = {
            "MATCH (n) RETURN n LIMIT $limit",
            "MATCH (n) RETURN n LIMIT 10 * 2000",
            "MATCH (n) RETURN n LIMIT coalesce(10000, 1)",
            "CALL db.labels() YIELD label RETURN label",
            "MATCH (n) RETURN n UNION MATCH (m) RETURN m",
            "CREATE (n) RETURN n",
            "MATCH (n) SET n.name = 'Alice' RETURN n",
            "MATCH (n) WITH collect(n) AS nodes CREATE (m) RETURN nodes",
            "MATCH (n) RETURN collect(customReadFunction(n))"
        };
        for (String query : queries) {
            this.assertPreview(query, query, false);
        }
    }

    @Test
    public void acceptsUnwindReadsButStillRejectsWrites() {
        ReadOnlyCypher.validate("UNWIND [1, 2] AS item RETURN item");
        ReadOnlyCypher.validate("MATCH (n) UNWIND n.items AS item RETURN item");
        ReadOnlyCypher.validate("UNWIND range(1, 10) AS n WITH n LIMIT 5 RETURN n");
        this.assertRejected(new String[]{
            "UNWIND [1, 2] AS item CREATE (n) RETURN n",
            "MATCH (n) UNWIND n.items AS item SET n.name = item RETURN n"
        });
    }

    @Test
    public void acceptsReadClausesPatternsAndAggregations() {
        String[] queries = {
            "MATCH (n) RETURN n LIMIT 10",
            "match (n:Person) where n.age >= 18 return n.name as name order by name asc skip 0 limit 10;",
            "MATCH (n:Person {name: 'Alice'})-[:KNOWS]->(friend) RETURN friend",
            "MATCH (a)<-[r:KNOWS]-(b), (c) RETURN a, r, b, c",
            "MATCH p=(a:Person)-[r:KNOWS|:WORKS_WITH*1..3]->(b) RETURN p, relationships(p)",
            "MATCH (a)-[:KNOWS*..3]-(b) RETURN a, b",
            "MATCH (a)-[*3]-(b) RETURN a, b",
            "MATCH (n:Person:Employee) RETURN n",
            "MATCH (n) OPTIONAL MATCH (n)-[r]->(m) WHERE m.active = true RETURN n, collect(m), count(r)",
            "MATCH (n) WITH n, count(*) AS total WHERE total > 0 MATCH (n)-->(m) RETURN m, total",
            "MATCH (n) WITH n ORDER BY n.age DESC LIMIT 20 WHERE n.age > 0 " +
                "WITH n.name AS name, sum(n.age) AS total RETURN name, total ORDER BY total DESCENDING",
            "MATCH (n) RETURN DISTINCT n.name ORDER BY n.name ASCENDING, n.age DESC SKIP 0 LIMIT 10",
            "MATCH (用户:人员 {姓名: '张三'}) RETURN 用户.姓名 AS 名字",
            "MATCH (n) WHERE n.age IN [18, 19, 20] AND NOT n.name IS NULL RETURN n",
            "MATCH (n) WHERE n.name STARTS WITH 'A' OR n.name ENDS WITH 'z' RETURN n",
            "MATCH (n) WHERE n.name CONTAINS 'a' AND n.name =~ 'A.*' RETURN n",
            "MATCH (n) WHERE n.age / 2 + 3 * 4 % 2 ^ 2 <> -1 AND n.valid != false RETURN n",
            "RETURN 1, -2.5e+2, true, false, null, [1, 2][0], {name: 'Alice'}.name",
            "RETURN '$name'",
            "RETURN count(*), count(DISTINCT 1), coalesce(null, 'fallback'), substring('Alice', 0, 2)",
            "MATCH (n) RETURN count(n) AS count",
            "MATCH (n) RETURN n.count AS collect, n.exists AS exists, n.collect AS count",
            "MATCH (e:Event) RETURN e.start, e.end",
            "MATCH (n {end: 1}) RETURN n",
            "MATCH (n:START)-[:END|:CASE]->(m:SHOW) RETURN n.contains, {database: m.all}",
            "MATCH (n:`RETURN`)-[:`CREATE`]->(m) RETURN n.`DELETE`",
            "WITH 1 AS number RETURN number",
            "MATCH (n) WITH * RETURN *"
        };
        for (String query : queries) {
            ReadOnlyCypher.validate(query);
        }
    }

    @Test
    public void acceptsQuotedKeywordsAndExplicitStringEscapes() {
        String[] queries = {
            "RETURN 'CALL apoc.doIt() SET DELETE', \"MATCH // /* */\"",
            "RETURN 'O\\'Brien', \"a\\\"b\", 'a\\\\b', 'a\\nb\\tc\\rd\\be\\ff'",
            "MATCH (`CALL`:`SET` {`DELETE`: 'MERGE'}) RETURN `CALL`.`DELETE` AS `RETURN`",
            "MATCH (`a``b`:`中文标签`) RETURN `a``b`",
            "RETURN '//', '/*', '*/', 'http://localhost/', 'semi;colon'"
        };
        for (String query : queries) {
            ReadOnlyCypher.validate(query);
        }
    }

    @Test
    public void rejectsWritesProceduresSubqueriesAndMultipleStatements() {
        String[] queries = {
            "CREATE (n) RETURN n", "MERGE (n) RETURN n", "MATCH (n) SET n.name = 'bad' RETURN n",
            "MATCH (n:START) SET n.end = 1 RETURN n",
            "MATCH (n) REMOVE n.name RETURN n", "MATCH (n) DELETE n RETURN n",
            "MATCH (n) DETACH DELETE n RETURN n", "DROP DATABASE hugegraph",
            "CALL db.labels()", "CALL db.labels() YIELD label RETURN label",
            "MATCH (n) CALL { MATCH (m) RETURN m } RETURN n",
            "LOAD CSV FROM 'file:///etc/passwd' AS line RETURN line",
            "USE hugegraph MATCH (n) RETURN n", "FOREACH (n IN [1] | CREATE ()) RETURN 1",
            "MATCH (n) RETURN n UNION MATCH (m) RETURN m",
            "RETURN 1 UNION ALL RETURN 2", "RETURN 1; RETURN 2", "RETURN 1;;",
            "match (n) detach delete n return n", "Match (n) Set n.x = 1 Return n",
            "MATCH (start) RETURN start", "RETURN 1 AS end",
            "MATCH (n) RETURN n CREATE (m)", "MATCH (n) RETURN n WITH n RETURN n",
            "MATCH (n) RETURN n MATCH (m) RETURN m", "RETURN 1 WHERE true",
            "MATCH (n) WHERE EXISTS { MATCH (m) RETURN m } RETURN n",
            "RETURN COUNT { MATCH (m) RETURN m }", "RETURN COLLECT { MATCH (m) RETURN m }",
            "RETURN exists((MATCH (n) RETURN n))", "RETURN [x IN [1] | x]",
            "RETURN [(n)-->(m) | m]", "MATCH (n) RETURN n {.*}",
            "EXPLAIN MATCH (n) RETURN n", "PROFILE MATCH (n) RETURN n"
        };
        this.assertRejected(queries);
    }

    @Test
    public void rejectsUnknownFunctionsAndNamespaceCalls() {
        String[] queries = {
            "RETURN apoc.cypher.run('CREATE ()', {})", "RETURN customReadFunction(1)",
            "RETURN db.labels()", "RETURN n.method()", "RETURN `count`(1)",
            "RETURN COUNT { CREATE () RETURN 1 }", "RETURN exists({MATCH (n) RETURN n})",
            "RETURN sum(*)", "RETURN count(1).toString()", "RETURN java.lang.Runtime.getRuntime()",
            "RETURN apoc.`cypher`.`run`('CREATE ()', {})", "RETURN date()"
        };
        this.assertRejected(queries);
    }

    @Test
    public void rejectsIncompleteSyntaxCommentsAndEscapes() {
        String[] queries = {
            "", "MATCH (n)", "RETURN", "RETURN 1 payload", "MATCH (n) RETURN n payload",
            "MATCH (n) WHERE RETURN n", "MATCH (n) RETURN n ORDER n", "RETURN 1 LIMIT",
            "RETURN 1 LIMIT 2 ORDER BY 1", "MATCH (n) RETURN ,n", "RETURN 1,", "RETURN (1",
            "RETURN [1,]", "RETURN {name:}", "RETURN {name: 1,}", "RETURN count(,)",
            "RETURN count(1,)", "RETURN 1 +", "RETURN 2e", "RETURN 1 ! 2", "RETURN 1..2",
            "MATCH (n)-[r:TYPE*1.5]-(m) RETURN n", "MATCH (n)<-->(m) RETURN n",
            "RETURN 1 // comment", "RETURN 1 /* comment */", "RETURN 1 */",
            "RETURN 1 / **/ 2", "RETURN 1 /\n/ 2", "MATCH/**/(n) RETURN n",
            "RETURN 'unterminated", "RETURN \"unterminated", "RETURN `unterminated",
            "RETURN 'a\\q'", "RETURN '\\" + "u0027'", "RETURN '\\U00000027'",
            "RETURN `a\\u0060`", "RETURN `a\\`b`", "RETURN ``", "RETURN 'a\nb'",
            "RET\\u0055RN 1", "RETURN\u00a01", "RETURN\u20031", "RETURN 1\u0000",
            "RETURN 'ok' 'SET n.bad = 1'", "RETURN `safe` CALL db.labels()"
        };
        this.assertRejected(queries);
        Assert.assertThrows(ExternalException.class, () -> ReadOnlyCypher.validate(null));
    }

    @Test
    public void rejectsUnboundParameters() {
        this.assertRejected(new String[]{
            "MATCH (n:Person:Employee $props) RETURN n",
            "MATCH (n) RETURN n SKIP $offset LIMIT $limit",
            "MATCH (n) WHERE n.name = $name RETURN n"
        });
    }

    @Test
    public void boundsLengthTokenCountAndNesting() {
        this.assertRejected(new String[]{
            "RETURN '" + "x".repeat(10000) + "'",
            "RETURN " + "1+".repeat(2100) + "1",
            "RETURN " + "(".repeat(40) + "1" + ")".repeat(40),
            "RETURN " + "[".repeat(40) + "1" + "]".repeat(40),
            "RETURN " + "coalesce(".repeat(40) + "1" + ")".repeat(40),
            "RETURN " + "NOT ".repeat(40) + "true"
        });
    }

    @Test
    public void requiresFinitePathBoundsWithinHopLimit() {
        String[] lengths = {"0", "3", "10", "0..0", "1..3", "0..10", "1..10", "..3", "..10"};
        for (String length : lengths) {
            ReadOnlyCypher.validate("MATCH (a)-[:KNOWS*" + length + "]->(b) RETURN a, b");
        }
        String[] invalidLengths = {
            "", "1..", "..", "11", "1..11", "..11", "11..11", "3..1", "..0", "-1", "-0",
            "-1..3", "1..-3", "1.5", "1..3.5", "1e0", "1..3e0", "999999999999999999999999"
        };
        for (String length : invalidLengths) {
            this.assertRejected(new String[]{"MATCH (a)-[:KNOWS*" + length + "]->(b) RETURN a, b"});
        }
    }

    private void assertRejected(String[] queries) {
        for (String query : queries) {
            Throwable error = Assert.assertThrows(ExternalException.class, () -> ReadOnlyCypher.validate(query));
            Assert.assertEquals("text2gql.cypher.read-only", error.getMessage());
            Assert.assertNull(error.getCause());
        }
    }

    private void assertPreview(String query, String expected, boolean defaultApplied) {
        ReadOnlyCypher.PreviewQuery preview = ReadOnlyCypher.preview(query, 300, 1001);
        Assert.assertEquals(expected, preview.query());
        Assert.assertEquals(defaultApplied, preview.defaultLimitApplied());
    }
}
