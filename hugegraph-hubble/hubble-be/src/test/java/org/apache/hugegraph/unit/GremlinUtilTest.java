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

import org.junit.Test;

import org.apache.hugegraph.testutil.Assert;
import org.apache.hugegraph.util.GremlinUtil;

public class GremlinUtilTest {

    private static final int LIMIT = 250;

    @Test
    public void testOneToOneMappersAndLocalLimitsPreserveAnEarlierGlobalBound() {
        for (String mapper : new String[]{"id()", "label()", "path()", "project('name').by('name')",
                                          "select('x')", "valueMap()", "elementMap()"}) {
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
