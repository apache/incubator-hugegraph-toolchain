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
