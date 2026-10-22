/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hugegraph.unit;

import java.util.List;
import java.util.Set;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.apache.tinkerpop.gremlin.jsr223.GremlinLangScriptEngine;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.Test;

import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.util.GremlinUtil;

public class GremlinUtilTest {

    private static final int LIMIT = 250;

    @Test
    public void testRewrittenQueriesOnAnActualTraversalEngine() throws Exception {
        try (TinkerGraph graph = TinkerGraph.open()) {
            for (int i = 0; i < 1500; i++) {
                graph.addVertex("number", i);
            }
            Bindings bindings = new SimpleBindings();
            bindings.put("g", graph.traversal());
            GremlinLangScriptEngine engine = new GremlinLangScriptEngine();
            Assert.assertEquals(300, execute(engine, bindings, GremlinUtil.optimizeLimit("g.V()", 300)).size());
            String explicit = "g.V().limit(10000L)";
            Assert.assertEquals(1500, execute(engine, bindings, GremlinUtil.optimizeLimit(explicit, 300)).size());
            Assert.assertEquals(1001, execute(engine, bindings,
                                            GremlinUtil.optimizePreviewLimit(explicit, 1001)).size());
            String unboundedRange = "g.V().range(0, -1)";
            Assert.assertEquals(1500, execute(engine, bindings, unboundedRange).size());
            Assert.assertEquals(300, execute(engine, bindings,
                                           GremlinUtil.optimizeLimit(unboundedRange, 300)).size());
            String localCount = "g.V().valueMap().count(Scope.local)";
            Assert.assertEquals(1500, execute(engine, bindings, localCount).size());
            Assert.assertEquals(300, execute(engine, bindings, GremlinUtil.optimizeLimit(localCount, 300)).size());
            Assert.assertEquals(1001, execute(engine, bindings,
                                            GremlinUtil.optimizePreviewLimit(localCount, 1001)).size());
            String boundedLocalCount = "g.V().limit(1000).valueMap().count(Scope.local)";
            Assert.assertEquals(1000, execute(engine, bindings,
                                            GremlinUtil.optimizeLimit(boundedLocalCount, 300)).size());
            String expanded = "g.V().fold().unfold()";
            Assert.assertEquals(300, execute(engine, bindings, GremlinUtil.optimizeLimit(expanded, 300)).size());
            Assert.assertEquals(List.of(1500L), execute(engine, bindings,
                                                       GremlinUtil.optimizeLimit("g.V().count()", 300)));
            Object materialized = engine.eval(GremlinUtil.optimizeLimit("g.V().toList()", 300), bindings);
            Assert.assertEquals(300, ((List<?>) materialized).size());
            Object preview = engine.eval(GremlinUtil.optimizePreviewLimit(explicit + ".toList()", 1001), bindings);
            Assert.assertEquals(1001, ((List<?>) preview).size());
            Object set = engine.eval(GremlinUtil.optimizeLimit("g.V().toSet()", 300), bindings);
            Assert.assertEquals(300, ((Set<?>) set).size());
            Object next = engine.eval(GremlinUtil.optimizeLimit("g.V().next()", 300), bindings);
            Assert.assertTrue(next instanceof Vertex);
            Object batch = engine.eval(GremlinUtil.optimizeLimit("g.V().next(10000)", 300), bindings);
            Assert.assertEquals(300, ((List<?>) batch).size());
            Assert.assertEquals(true, engine.eval(GremlinUtil.optimizeLimit("g.V().hasNext()", 300), bindings));
            Assert.assertEquals(List.of(1500L),
                                engine.eval(GremlinUtil.optimizeLimit("g.V().count().toList()", 300), bindings));
        }
    }

    @Test
    public void testBareAnonymousTraversalsReceiveEffectiveOuterLimits() throws Exception {
        try (TinkerGraph graph = TinkerGraph.open()) {
            Vertex previous = null;
            for (int i = 0; i < 1500; i++) {
                Vertex vertex = graph.addVertex("name", "vertex-" + i);
                if (previous != null) {
                    previous.addEdge("next", vertex);
                    previous.addEdge("next", vertex);
                    previous.addEdge("next", vertex);
                }
                previous = vertex;
            }
            Bindings bindings = new SimpleBindings();
            bindings.put("g", graph.traversal());
            GremlinLangScriptEngine engine = new GremlinLangScriptEngine();
            for (String query : new String[]{"g.V().repeat(out()).times(2).path()",
                                             "g.V().repeat(__.out()).times(2).path()",
                                             "g.V().where(out().count().is(gt(2))).out()",
                                             "g.V().where(__.out().count().is(gt(2))).out()"}) {
                Assert.assertTrue(execute(engine, bindings, query).size() > 1001);
                String ordinary = GremlinUtil.optimizeLimit(query, 300);
                String preview = GremlinUtil.optimizePreviewLimit(query, 1001);
                Assert.assertEquals(query + ".limit(300)", ordinary);
                Assert.assertEquals(query + ".limit(1001)", preview);
                Assert.assertEquals(300, execute(engine, bindings, ordinary).size());
                Assert.assertEquals(1001, execute(engine, bindings, preview).size());
            }
        }
        String textPredicate = "g.V().has('name', Text.contains('x')).out()";
        Assert.assertEquals(textPredicate + ".limit(300)", GremlinUtil.optimizeLimit(textPredicate, 300));
        Assert.assertEquals(textPredicate + ".limit(1001)",
                            GremlinUtil.optimizePreviewLimit(textPredicate, 1001));
    }

    private static List<?> execute(GremlinLangScriptEngine engine, Bindings bindings,
                                   String query) throws Exception {
        return ((Traversal<?, ?>) engine.eval(query, bindings)).toList();
    }

    @Test
    public void testOneToOneMappersAndLocalLimitsPreserveAnEarlierGlobalBound() {
        for (String mapper : new String[]{"id()", "label()", "path()", "project('name').by('name')",
                                          "select('x')", "valueMap()", "elementMap()", "propertyMap()",
                                          "key()", "value()", "constant(1)", "map(__.out())", "skip(1)"}) {
            String query = "g.V().as('x').limit(1000)." + mapper;
            Assert.assertEquals(query, GremlinUtil.optimizeLimit(query, 300));
            Assert.assertEquals(query + ".limit(1001)", GremlinUtil.optimizePreviewLimit(query, 1001));
            for (String expansion : new String[]{".unfold()", ".out()"}) {
                Assert.assertEquals(query + expansion + ".limit(300)",
                                    GremlinUtil.optimizeLimit(query + expansion, 300));
            }
        }
        for (String local : new String[]{"limit(local, 5)", "range(Scope.local, 0, 5)",
                                         "sample(local, 5)", "tail(Scope.local, 5)"}) {
            String query = "g.V().limit(1000)." + local;
            Assert.assertEquals(query, GremlinUtil.optimizeLimit(query, 300));
            Assert.assertEquals(query + ".limit(1001)", GremlinUtil.optimizePreviewLimit(query, 1001));
            String unbounded = "g.V()." + local;
            Assert.assertEquals(unbounded + ".limit(300)", GremlinUtil.optimizeLimit(unbounded, 300));
        }
    }

    @Test
    public void testGroovyIntegerBoundsAndNumericArgumentsUseTheSameGrammar() {
        for (String literal : new String[]{"10000L", "100_000", "100__000", "10_000G", "10_000I",
                                            "0x2710L", "0b1_0000", "023420", "0_10", "10000l",
                                            "10000g", "10000i", "+10000"}) {
            String bounded = "g.V().limit(" + literal + ")";
            Assert.assertEquals(bounded, GremlinUtil.optimizeLimit(bounded, 300));
            Assert.assertEquals(bounded + ".limit(1001)",
                                GremlinUtil.optimizePreviewLimit(bounded, 1001));
            // A large numeric filter value must not bypass either limit.
            String filtered = "g.V().has('number', " + literal + ")";
            Assert.assertEquals(filtered + ".limit(300)", GremlinUtil.optimizeLimit(filtered, 300));
            Assert.assertEquals(filtered + ".limit(1001)", GremlinUtil.optimizePreviewLimit(filtered, 1001));
        }
    }

    @Test
    public void testOnlyFiniteOuterBoundsSuppressTheDefault() {
        for (String step : new String[]{"limit(-1)", "range(0, -1)", "range(Scope.global, 0L, -1L)",
                                         "sample(-1)", "tail(-1)", "limit(local, 10_000)",
                                         "range(Scope.local, 0, 10_000)"}) {
            String unbounded = "g.V()." + step;
            Assert.assertEquals(unbounded + ".limit(300)", GremlinUtil.optimizeLimit(unbounded, 300));
            String bounded = "g.V().limit(20)." + step;
            Assert.assertEquals(bounded, GremlinUtil.optimizeLimit(bounded, 300));
        }
        for (String step : new String[]{"tail()", "tail(Scope.global)", "range(0, 10_000L)",
                                         "limit(global, 10_000)", "sample(10_000I)"}) {
            String query = "g.V()." + step;
            Assert.assertEquals(query, GremlinUtil.optimizeLimit(query, 300));
            Assert.assertEquals(query + ".limit(1001)", GremlinUtil.optimizePreviewLimit(query, 1001));
        }
        String nested = "g.V().where(__.out().limit(10_000))";
        Assert.assertEquals(nested + ".limit(300)", GremlinUtil.optimizeLimit(nested, 300));
    }

    @Test
    public void testFinalConsumersReceiveLimitsBeforeMaterialization() {
        for (String consumer : new String[]{"toList()", "toSet()", "next()", "next(10_000)", "hasNext()"}) {
            String query = "g.V()." + consumer;
            Assert.assertEquals("g.V().limit(300)." + consumer, GremlinUtil.optimizeLimit(query, 300));
            Assert.assertEquals("g.V().limit(1001)." + consumer, GremlinUtil.optimizePreviewLimit(query, 1001));
            String bounded = "g.V().limit(10000L)." + consumer;
            Assert.assertEquals(bounded, GremlinUtil.optimizeLimit(bounded, 300));
            Assert.assertEquals("g.V().limit(10000L).limit(1001)." + consumer,
                                GremlinUtil.optimizePreviewLimit(bounded, 1001));
            String count = "g.V().count()." + consumer;
            Assert.assertEquals(count, GremlinUtil.optimizeLimit(count, 300));
            Assert.assertEquals(count, GremlinUtil.optimizePreviewLimit(count, 1001));
        }
        for (String query : new String[]{"g.V().toList().size()", "g.V().next().value('name')"}) {
            Assert.assertEquals(query, GremlinUtil.optimizeLimit(query, 300));
            Assert.assertEquals(query, GremlinUtil.optimizePreviewLimit(query, 1001));
        }
    }

    @Test
    public void testLocalReducersDoNotBypassResultLimits() {
        for (String query : new String[]{"g.V().valueMap().count(local)",
                                         "g.V().values('scores').sum(Scope.local)",
                                         "g.V().values('scores').min(local)",
                                         "g.V().values('scores').max(Scope.local)",
                                         "g.V().values('scores').mean(local)",
                                         "g.V().fold().unfold().valueMap().count(local)"}) {
            Assert.assertEquals(query + ".limit(300)", GremlinUtil.optimizeLimit(query, 300));
            Assert.assertEquals(query + ".limit(1001)", GremlinUtil.optimizePreviewLimit(query, 1001));
        }
        for (String query : new String[]{"g.V().count()", "g.V().count(global)",
                                         "g.V().values('age').sum(Scope.global)"}) {
            Assert.assertEquals(query, GremlinUtil.optimizeLimit(query, 300));
            Assert.assertEquals(query, GremlinUtil.optimizePreviewLimit(query, 1001));
        }
        for (String bounded : new String[]{"g.V().limit(10).count(local)",
                                           "g.V().limit(10).valueMap().count(local)",
                                           "g.V().limit(10).elementMap().count(Scope.local)"}) {
            Assert.assertEquals(bounded, GremlinUtil.optimizeLimit(bounded, 300));
            Assert.assertEquals(bounded + ".limit(1001)", GremlinUtil.optimizePreviewLimit(bounded, 1001));
        }
        String expanding = "g.V().limit(10).valueMap().unfold()";
        Assert.assertEquals(expanding + ".limit(300)", GremlinUtil.optimizeLimit(expanding, 300));
        Assert.assertEquals("g.V().valueMap().count(local /* scope */).limit(300)",
                            GremlinUtil.optimizeLimit("g.V().valueMap().count(local /* scope */)", 300));
    }

    @Test
    public void testExpansionAfterBarrierReceivesFinalLimits() {
        for (String query : new String[]{"g.V().fold().unfold().out()",
                                         "g.V().groupCount().by(label).unfold()",
                                         "g.V().fold().limit(1).unfold().out()"}) {
            Assert.assertEquals(query + ".limit(300)", GremlinUtil.optimizeLimit(query, 300));
            Assert.assertEquals(query + ".limit(1001)", GremlinUtil.optimizePreviewLimit(query, 1001));
        }
        for (String query : new String[]{"g.V().count()", "g.V().fold()",
                                         "g.V().groupCount().by(label)", "g.V().values('age').sum()",
                                         "g.V().property('x', 1).fold().unfold().out()"}) {
            Assert.assertEquals(query, GremlinUtil.optimizeLimit(query, 300));
            Assert.assertEquals(query, GremlinUtil.optimizePreviewLimit(query, 1001));
        }
    }

    @Test
    public void testPreviewCapUsesReadGrammarAndPreservesComments() {
        Assert.assertEquals("g.V().path().limit(12000).limit(1001) // preview",
                            GremlinUtil.optimizePreviewLimit(
                            "g.V().path().limit(12000) // preview", 1001));
        Assert.assertEquals("g.V().count()",
                            GremlinUtil.optimizePreviewLimit("g.V().count()", 1001));
        Assert.assertEquals("g.V().property('x', 1).out()",
                            GremlinUtil.optimizePreviewLimit("g.V().property('x', 1).out()", 1001));
    }

    @Test
    public void testDefaultLimitForOrdinaryReads() {
        for (String query : new String[]{
                "g.V().has('name', 'x')",
                "g.V().local(__.out().limit(2))",
                "g.V().limit(2).out()",
                "g.V().limit(1).local(__.out())",
                "g.V().limit(1).values('a', 'b')",
                "g.V().valueMap().limit(local, 1)",
                "g.V().valueMap().range(Scope.local, 0, 1)"}) {
            Assert.assertEquals(query + ".limit(300)",
                                GremlinUtil.optimizeLimit(query, 300));
        }
    }

    @Test
    public void testDefaultLimitPreservesExplicitLimitsAndSemantics() {
        for (String query : new String[]{
                "g.V().limit(10000)", "g.V().limit(10000).hasLabel('person')",
                "g.V().range(10, 10000)", "g.V().count()",
                "g.V().groupCount().by(label)", "g.V().values('age').sum()",
                "g.V().property('x', 1).out()", "g.V().drop()",
                "g.addV('person').out()", "def x = g.V().out()"}) {
            Assert.assertEquals(query, GremlinUtil.optimizeLimit(query, 300));
        }
    }

    @Test
    public void testSuffixAppend() {
        // Matched suffixes get '.limit(N)' appended
        Assert.assertEquals("g.V().limit(250)",
                            GremlinUtil.optimizeLimit("g.V()", LIMIT));
        Assert.assertEquals("g.V().out().limit(250)",
                            GremlinUtil.optimizeLimit("g.V().out()", LIMIT));
        Assert.assertEquals("g.E().limit(250)",
                            GremlinUtil.optimizeLimit("g.E()", LIMIT));
        Assert.assertEquals("g.V().hasLabel('person').limit(250)",
                            GremlinUtil.optimizeLimit(
                            "g.V().hasLabel('person')", LIMIT));
        // Unmatched suffixes are left untouched
        Assert.assertEquals("g.V().count()",
                            GremlinUtil.optimizeLimit("g.V().count()", LIMIT));
        // Multi-line: each line is optimized independently
        Assert.assertEquals("g.V().limit(250)\ng.V().count()",
                            GremlinUtil.optimizeLimit(
                            "g.V()\ng.V().count()", LIMIT));
    }

    @Test
    public void testMultilineChainReceivesOnlyOneTailLimit() {
        Assert.assertEquals("g.V()\n  .hasLabel(\"person\")\n  " +
                            ".out().limit(250)",
                            GremlinUtil.optimizeLimit(
                            "g.V()\n  .hasLabel(\"person\")\n  .out()",
                            LIMIT));
        Assert.assertEquals("g.V() // continue\n  .out().limit(250)",
                            GremlinUtil.optimizeLimit(
                            "g.V() // continue\n  .out()", LIMIT));
        Assert.assertEquals("g.V()\n/* keep traversing */\n" +
                            ".out().limit(250)",
                            GremlinUtil.optimizeLimit(
                            "g.V()\n/* keep traversing */\n.out()",
                            LIMIT));
    }

    @Test
    public void testApostropheInCommentDoesNotDisableLimit() {
        // An apostrophe inside a comment must not be read as a string
        // delimiter, otherwise the rest of the script is treated as one
        // unterminated string and never receives its limit.
        String optimized = GremlinUtil.optimizeLimit(
                "// don't limit this\ng.V()", 250);
        Assert.assertTrue(optimized, optimized.endsWith(".limit(250)"));
        Assert.assertTrue(optimized, optimized.startsWith("// don't limit"));
    }

    @Test
    public void testQuoteInCommentDoesNotLeakIntoNextStatement() {
        String optimized = GremlinUtil.optimizeLimit(
                "// it's here\ng.V();g.E()", 10);
        Assert.assertEquals("// it's here\ng.V().limit(10);g.E().limit(10)",
                            optimized);
    }

    @Test
    public void testIgnoredCommentLine() {
        Assert.assertEquals("// g.V()\ng.E().limit(250)",
                            GremlinUtil.optimizeLimit(
                            "// g.V()\ng.E()", LIMIT));
    }

    @Test
    public void testTrailingLineCommentKeepsLimitBeforeComment() {
        Assert.assertEquals("g.V().limit(250) // trailing",
                            GremlinUtil.optimizeLimit(
                            "g.V() // trailing", LIMIT));
    }

    @Test
    public void testBlockCommentDoesNotAffectStatementParsing() {
        Assert.assertEquals("g.V().limit(250) /* '; \\\" */;" +
                            "g.E().limit(250)",
                            GremlinUtil.optimizeLimit(
                            "g.V() /* '; \\\" */;g.E()", LIMIT));
        Assert.assertEquals("g.V().limit(250) /* line one\n" +
                            "line two */\ng.E().limit(250)",
                            GremlinUtil.optimizeLimit(
                            "g.V() /* line one\nline two */\ng.E()", LIMIT));
    }

    @Test
    public void testPlainMultiStatement() {
        Assert.assertEquals("g.V().limit(250);g.E().limit(250)",
                            GremlinUtil.optimizeLimit("g.V();g.E()", LIMIT));
        Assert.assertEquals("g.V().count();g.E().limit(250)",
                            GremlinUtil.optimizeLimit(
                            "g.V().count();g.E()", LIMIT));
        // Trailing separator kept, no limit appended to the empty tail
        Assert.assertEquals("g.V().limit(250);",
                            GremlinUtil.optimizeLimit("g.V();", LIMIT));
    }

    @Test
    public void testSemicolonInsideSingleQuotedString() {
        // The ';' inside the quoted string is not a statement separator
        Assert.assertEquals("g.V().hasLabel('a;b').limit(250)",
                            GremlinUtil.optimizeLimit(
                            "g.V().hasLabel('a;b')", LIMIT));
        Assert.assertEquals("g.V().hasLabel('a;b').limit(250);" +
                            "g.E().limit(250)",
                            GremlinUtil.optimizeLimit(
                            "g.V().hasLabel('a;b');g.E()", LIMIT));
    }

    @Test
    public void testSemicolonInsideDoubleQuotedString() {
        Assert.assertEquals("g.V().hasLabel(\"a;b\").limit(250)",
                            GremlinUtil.optimizeLimit(
                            "g.V().hasLabel(\"a;b\")", LIMIT));
    }

    @Test
    public void testEscapedQuoteInsideString() {
        // The escaped quote does not close the string, so the ';' and the
        // fake suffix inside it are ignored
        Assert.assertEquals("g.V().hasLabel('it\\'s;x').limit(250)",
                            GremlinUtil.optimizeLimit(
                            "g.V().hasLabel('it\\'s;x')", LIMIT));
        Assert.assertEquals("g.V().hasLabel(\"say \\\";\\\" ok\").limit(250)",
                            GremlinUtil.optimizeLimit(
                            "g.V().hasLabel(\"say \\\";\\\" ok\")", LIMIT));
    }

    @Test
    public void testNewlineInsideStringNotSeparator() {
        // A '\n' inside quotes must not be treated as a line separator
        String gremlin = "g.V().hasLabel('a\nb')";
        Assert.assertEquals("g.V().hasLabel('a\nb').limit(250)",
                            GremlinUtil.optimizeLimit(gremlin, LIMIT));
    }

    @Test
    public void testUnclosedQuoteNotOptimized() {
        // Never append '.limit(N)' inside an unterminated string literal
        String gremlin = "g.V().hasLabel('unclosed";
        Assert.assertEquals(gremlin,
                            GremlinUtil.optimizeLimit(gremlin, LIMIT));
    }

    @Test
    public void testUnsupportedGroovyLiteralsAreNotOptimized() {
        String slashy = "def x = /g.V()\\n/\ng.E()";
        Assert.assertEquals(slashy,
                            GremlinUtil.optimizeLimit(slashy, LIMIT));

        String dollarSlashy = "def x = $/g.V()/$\ng.E()";
        Assert.assertEquals(dollarSlashy,
                            GremlinUtil.optimizeLimit(dollarSlashy, LIMIT));

        String tripleQuoted = "def x = '''g.V()\ng.E()'''\ng.V()";
        Assert.assertEquals(tripleQuoted,
                            GremlinUtil.optimizeLimit(tripleQuoted, LIMIT));

        String division = "def half = 4 / 2\ng.V()";
        Assert.assertEquals(division,
                            GremlinUtil.optimizeLimit(division, LIMIT));
    }
}
