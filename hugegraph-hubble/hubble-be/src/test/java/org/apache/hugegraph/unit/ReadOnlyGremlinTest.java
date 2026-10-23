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
import org.apache.hugegraph.util.ReadOnlyGremlin;
import org.junit.Test;

public class ReadOnlyGremlinTest {

    @Test
    public void acceptsReadTraversalsAndNestedPredicates() {
        String[] queries = {
                "g.V().limit(10)",
                "  g.V().count(); \n",
                "g.E().hasLabel('knows').inV().values('name')",
                "g.V().has('age', P.gte(18).and(P.lt(65))).count()",
                "g.V().has('name', TextP.startingWith('A')).elementMap()",
                "g.V().has('name', within(['Alice', 'Bob'])).valueMap(true)",
                "g.V().has('age', between(-2.5, 3e2)).order().by('age', Order.desc)",
                "g.V().project('name', 'friends').by('name').by(__.out('knows').count())",
                "g.V().repeat(__.out('knows')).times(2).path().by('name')",
                "g.V().repeat(out()).times(2).path()",
                "g.V().where(out().count().is(gt(2))).out()",
                "g.V().project('name').by(values ('name'))",
                "g.V().has('name', Text.contains('x')).out()",
                "g.V().has('name', Text.contains('x').and(P.neq('y'))).out()",
                "g.V().groupCount().by('name').order(Scope.local).by(Column.values, desc)",
                "g.V().order().by(label).by(id)",
                "g.V().group().by(key).by(value)",
                "g.V().has('name', 'O\\'Brien').values('name');",
                "g.V().has('name', '价格$10').valueMap()",
                "g.V().has(\"name\", \"Alice\").count()"
        };
        for (String query : queries) {
            ReadOnlyGremlin.validate(query);
        }
    }

    @Test
    public void rejectsWritesScriptsAndDynamicGroovy() {
        String[] queries = {
                "g.addV('person')", "g.V().drop()", "g.V().property('name','bad')",
                "g.V().sideEffect(__.drop())", "g.V().map(__.addV())",
                "g.V().repeat(drop()).times(2)", "g.V().where(out().drop())",
                "g.V().map(addV('person'))", "g.V().where(out().property('x', 1))",
                "g.V().has('name', Text.uuid('x'))", "g.V().has('name', Text.execute('x'))",
                "g.V(); g.V().drop()", "g.V()\ng.V().drop()",
                "g.V().filter{it.get().remove()}", "g.V().map({System.exit(0)})",
                "g.V().by(label.class)", "g.V().by(id.getClass())", "g.V().order().by(key.class)",
                "g.V().has('name', System.getProperty('user.home'))",
                "g.V().has('name', new File('/etc/passwd').text)",
                "g.V().has('name', 'anything'.execute())",
                "g.V().has('name', \"${g.V().drop()}\")",
                "g.V().has('name', \"$graph\")",
                "g.V().has('name', 'a' + 'b')", "g.V().has('name', /abc/)",
                "g.V().has('name', '''anything''')", "g.V().has('name', '\\u0027')",
                "g.V().has('name', 'unclosed)", "g.V().getClass()",
                "g.V().class.forName('java.lang.Runtime')", "g.V().has('id', T.id.class)",
                "g.V().withSideEffect('x', [])", "g.V().has('x', foo())",
                "g.V().has('x', within([g.V().drop()]))", "g.V().has('age', gt()) /* comment */",
                "g.V().has('age', -)", "g.V().has('age', 2e)", "g.V().has('age', 2.)",
                "g.V(", "g.V().has('x',)", "g.V() extra", "", "graph.traversal().V()"
        };
        for (String query : queries) {
            Assert.assertThrows(ExternalException.class, () -> ReadOnlyGremlin.validate(query));
        }
        Assert.assertThrows(ExternalException.class, () -> ReadOnlyGremlin.validate(null));
    }

    @Test
    public void acceptsGroovyNumericLiteralsWithoutAcceptingExpressions() {
        // Checked against the Groovy runtime used by HugeGraph Server. Numeric
        // syntax is validated here; the Server still validates step overloads.
        for (String literal : new String[]{"10000L", "100_000", "100__000", "10_000G", "10_000I",
                                            "0x2710L", "0b1_0000", "023420", "0_10", "10000l",
                                            "10000g", "10000i", "+10000", "-9223372036854775808L",
                                            "1e4", "1.0G", "10000D", "1_000.5_0e+1_0F"}) {
            ReadOnlyGremlin.validate("g.V().has('number', " + literal + ")");
        }
        for (String literal : new String[]{"08", "1_L", "1L0", "2147483648I", "9223372036854775808L",
                                            "1_", "0x_1", "0b2", "1e_2", "1.0L", "1 + 2",
                                            "Long.MAX_VALUE", "10000L.toString()"}) {
            Assert.assertThrows(ExternalException.class,
                                () -> ReadOnlyGremlin.validate("g.V().limit(" + literal + ")"));
        }
    }

    @Test
    public void boundsInputAndNesting() {
        Assert.assertThrows(ExternalException.class,
                     () -> ReadOnlyGremlin.validate("g.V().has('name','" + "x".repeat(10000) + "')"));
        String nested = "g.V().where(" + "__.where(".repeat(45) + "__.identity()" + ")".repeat(46);
        Assert.assertThrows(ExternalException.class, () -> ReadOnlyGremlin.validate(nested));
    }
}
