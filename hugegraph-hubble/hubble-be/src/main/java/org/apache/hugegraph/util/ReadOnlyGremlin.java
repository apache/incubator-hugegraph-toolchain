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

package org.apache.hugegraph.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hugegraph.exception.ExternalException;

/**
 * A deliberately small traversal grammar for automatically executed model output.
 * This accepts literal arguments, read steps and predicates, never Groovy scripts.
 * General scripts remain available through the ordinary Gremlin console.
 */
public final class ReadOnlyGremlin {

    // The grammar and result analysis share one definition of each supported step.
    // PRESERVE means at most one output per input; EXPAND is deliberately conservative.
    private static final Map<String, Output> STEPS = Map.ofEntries(
            Map.entry("V", Output.EXPAND),
            Map.entry("E", Output.EXPAND),
            Map.entry("out", Output.EXPAND),
            Map.entry("in", Output.EXPAND),
            Map.entry("both", Output.EXPAND),
            Map.entry("outE", Output.EXPAND),
            Map.entry("inE", Output.EXPAND),
            Map.entry("bothE", Output.EXPAND),
            Map.entry("bothV", Output.EXPAND),
            Map.entry("values", Output.EXPAND),
            Map.entry("properties", Output.EXPAND),
            Map.entry("unfold", Output.EXPAND),
            Map.entry("repeat", Output.EXPAND),
            Map.entry("local", Output.EXPAND),
            Map.entry("union", Output.EXPAND),
            Map.entry("coalesce", Output.EXPAND),
            Map.entry("choose", Output.EXPAND),
            Map.entry("optional", Output.EXPAND),
            Map.entry("flatMap", Output.EXPAND),
            Map.entry("match", Output.EXPAND),
            Map.entry("has", Output.PRESERVE),
            Map.entry("hasId", Output.PRESERVE),
            Map.entry("hasLabel", Output.PRESERVE),
            Map.entry("hasKey", Output.PRESERVE),
            Map.entry("hasValue", Output.PRESERVE),
            Map.entry("hasNot", Output.PRESERVE),
            Map.entry("outV", Output.PRESERVE),
            Map.entry("inV", Output.PRESERVE),
            Map.entry("otherV", Output.PRESERVE),
            Map.entry("valueMap", Output.PRESERVE),
            Map.entry("elementMap", Output.PRESERVE),
            Map.entry("propertyMap", Output.PRESERVE),
            Map.entry("label", Output.PRESERVE),
            Map.entry("id", Output.PRESERVE),
            Map.entry("key", Output.PRESERVE),
            Map.entry("value", Output.PRESERVE),
            Map.entry("dedup", Output.PRESERVE),
            Map.entry("order", Output.PRESERVE),
            Map.entry("by", Output.PRESERVE),
            Map.entry("skip", Output.PRESERVE),
            Map.entry("select", Output.PRESERVE),
            Map.entry("project", Output.PRESERVE),
            Map.entry("path", Output.PRESERVE),
            Map.entry("simplePath", Output.PRESERVE),
            Map.entry("cyclicPath", Output.PRESERVE),
            Map.entry("until", Output.PRESERVE),
            Map.entry("emit", Output.PRESERVE),
            Map.entry("times", Output.PRESERVE),
            Map.entry("loops", Output.PRESERVE),
            Map.entry("and", Output.PRESERVE),
            Map.entry("or", Output.PRESERVE),
            Map.entry("not", Output.PRESERVE),
            Map.entry("where", Output.PRESERVE),
            Map.entry("filter", Output.PRESERVE),
            Map.entry("map", Output.PRESERVE),
            Map.entry("identity", Output.PRESERVE),
            Map.entry("constant", Output.PRESERVE),
            Map.entry("as", Output.PRESERVE),
            Map.entry("is", Output.PRESERVE),
            Map.entry("count", Output.REDUCE),
            Map.entry("sum", Output.REDUCE),
            Map.entry("min", Output.REDUCE),
            Map.entry("max", Output.REDUCE),
            Map.entry("mean", Output.REDUCE),
            Map.entry("fold", Output.COLLECT),
            Map.entry("group", Output.COLLECT),
            Map.entry("groupCount", Output.COLLECT),
            Map.entry("limit", Output.LIMIT),
            Map.entry("range", Output.LIMIT),
            Map.entry("tail", Output.LIMIT),
            Map.entry("sample", Output.LIMIT),
            Map.entry("toList", Output.CONSUME),
            Map.entry("toSet", Output.CONSUME),
            Map.entry("next", Output.CONSUME),
            Map.entry("hasNext", Output.CONSUME));

    private static final Set<String> PREDICATES = Set.of(
            "eq", "neq", "gt", "gte", "lt", "lte", "inside", "outside", "between", "within", "without",
            "and", "or", "not", "startingWith", "notStartingWith", "endingWith", "notEndingWith",
            "containing", "notContaining", "regex", "notRegex");

    private static final Set<String> TEXT_PREDICATES = Set.of("contains");

    private static final Set<String> CONSTANTS = Set.of(
            "true", "false", "null", "asc", "desc", "shuffle", "local", "global", "keys", "values",
            "first", "last", "all", "mixed", "id", "label", "key", "value",
            "T.id", "T.label", "T.key", "T.value",
            "Order.asc", "Order.desc", "Order.shuffle", "Scope.local", "Scope.global",
            "Column.keys", "Column.values", "Pop.first", "Pop.last", "Pop.all", "Pop.mixed");

    private ReadOnlyGremlin() {
    }

    public static void validate(String query) {
        parse(query);
    }

    static Traversal parse(String query) {
        if (query == null || query.isBlank() || query.length() > 10000) {
            throw invalid();
        }
        Parser parser = new Parser(query);
        parser.expectIdentifier("g");
        parser.expect('.');
        String start = parser.identifier();
        if (!"V".equals(start) && !"E".equals(start)) {
            throw invalid();
        }
        List<Step> steps = new ArrayList<>();
        steps.add(new Step(start, parser.arguments(0), 0));
        parser.chain(STEPS.keySet(), 0, steps);
        parser.consume(';');
        parser.whitespace();
        if (parser.position != query.length()) {
            throw invalid();
        }
        return new Traversal(steps, query.length());
    }

    private enum Output {
        PRESERVE, EXPAND, REDUCE, COLLECT, LIMIT, CONSUME
    }

    static final class Traversal {

        private final List<Step> steps;
        private final int endPosition;

        private Traversal(List<Step> steps, int endPosition) {
            this.steps = List.copyOf(steps);
            this.endPosition = endPosition;
        }

        int limitPosition(boolean preview, int limit) {
            // Preview limits include the sentinel row used to report truncation.
            BigInteger previewSize = BigInteger.valueOf((long) limit - 1);
            Analysis result = this.analyze(previewSize);
            if (preview && result.largeCollection) {
                throw new ExternalException("gremlin.preview.collection_requires_async", previewSize);
            }
            return !result.singleResult && (preview || result.bound == null) ? result.position : -1;
        }

        private Analysis analyze(BigInteger previewSize) {
            BigInteger bound = null;
            boolean singleResult = false;
            boolean largeCollection = false;
            boolean nestedCollection = false;
            int position = this.endPosition;
            for (int i = 0; i < this.steps.size(); i++) {
                Step step = this.steps.get(i);
                switch (STEPS.get(step.name)) {
                    case CONSUME:
                        // Cap the traversal before its final consumer, preserving
                        // the List/Set/value/boolean result type. Further methods
                        // would operate on materialized results, outside this grammar.
                        if (i != this.steps.size() - 1) {
                            return new Analysis(-1, bound, true, false);
                        }
                        position = step.dotPosition;
                        if ("hasNext".equals(step.name)) {
                            largeCollection = false;
                            nestedCollection = false;
                        }
                        break;
                    case EXPAND:
                        bound = null;
                        singleResult = false;
                        largeCollection = false;
                        break;
                    case REDUCE:
                        largeCollection = false;
                        nestedCollection = false;
                        if (!step.localScope()) {
                            bound = BigInteger.ONE;
                            singleResult = true;
                        }
                        break;
                    case COLLECT:
                        // The keyed group overloads accumulate a side effect
                        // while passing each input traverser through.
                        if (("group".equals(step.name) || "groupCount".equals(step.name)) &&
                            !step.arguments.isEmpty()) {
                            break;
                        }
                        if (!step.localScope()) {
                            // A single List/Map can still download an entire
                            // graph. A later outer limit cannot shrink its input.
                            largeCollection |= bound == null || bound.compareTo(previewSize) > 0;
                            bound = BigInteger.ONE;
                            singleResult = true;
                        }
                        break;
                    case LIMIT:
                        BigInteger stepBound = step.finiteGlobalLimit();
                        if (stepBound != null) {
                            bound = bound == null ? stepBound : bound.min(stepBound);
                        }
                        break;
                    default:
                        break;
                }
                // Child traversals can put a large List/Map inside one outer
                // row. An outer limit cannot bound those collection inputs.
                for (Argument argument : step.arguments) {
                    if (argument.traversal != null) {
                        // Unfolding an outer Map can still return entries with
                        // unbounded child Lists; only scalar reducers clear this.
                        nestedCollection |= argument.traversal.analyze(previewSize).largeCollection;
                    }
                }
            }
            return new Analysis(position, bound, singleResult, largeCollection || nestedCollection);
        }
    }

    private record Analysis(int position, BigInteger bound, boolean singleResult, boolean largeCollection) {
    }

    private static final class Step {

        private final String name;
        private final List<Argument> arguments;
        private final int dotPosition;

        private Step(String name, List<Argument> arguments, int dotPosition) {
            this.name = name;
            this.arguments = List.copyOf(arguments);
            this.dotPosition = dotPosition;
        }

        private boolean localScope() {
            return !this.arguments.isEmpty() && this.arguments.get(0).localScope();
        }

        private BigInteger finiteGlobalLimit() {
            if (this.localScope()) {
                return null;
            }
            int offset = !this.arguments.isEmpty() && this.arguments.get(0).globalScope() ? 1 : 0;
            if ("tail".equals(this.name) && this.arguments.size() == offset) {
                return BigInteger.ONE;
            }
            int index = offset + ("range".equals(this.name) ? 1 : 0);
            if (index >= this.arguments.size()) {
                return null;
            }
            BigInteger value = this.arguments.get(index).integer;
            // In particular, range(0, -1) and limit(-1) are not finite bounds.
            if (value == null || value.signum() < 0 || value.bitLength() >= Long.SIZE) {
                return null;
            }
            if ("range".equals(this.name)) {
                BigInteger low = this.arguments.get(offset).integer;
                return low != null && low.signum() >= 0 && low.compareTo(value) <= 0 ?
                       value.subtract(low) : null;
            }
            return value;
        }
    }

    private static final class Argument {

        private static final Argument OTHER = new Argument(null, null, null);

        private final String constant;
        private final BigInteger integer;
        private final Traversal traversal;

        private Argument(String constant, BigInteger integer, Traversal traversal) {
            this.constant = constant;
            this.integer = integer;
            this.traversal = traversal;
        }

        private boolean localScope() {
            return "local".equals(this.constant) || "Scope.local".equals(this.constant);
        }

        private boolean globalScope() {
            return "global".equals(this.constant) || "Scope.global".equals(this.constant);
        }
    }

    private static ExternalException invalid() {
        return new ExternalException("text2gql.read-only");
    }

    private static final class Parser {

        private final String text;
        private int position;

        private Parser(String text) {
            this.text = text;
        }

        private void chain(Set<String> methods, int depth, List<Step> steps) {
            while (this.consume('.')) {
                int dotPosition = this.position - 1;
                String name = this.identifier();
                if (!methods.contains(name)) {
                    throw invalid();
                }
                List<Argument> arguments = this.arguments(depth);
                if (steps != null) {
                    steps.add(new Step(name, arguments, dotPosition));
                }
            }
        }

        private List<Argument> arguments(int depth) {
            if (depth > 40) {
                throw invalid();
            }
            this.expect('(');
            List<Argument> arguments = new ArrayList<>();
            if (!this.consume(')')) {
                do {
                    arguments.add(this.expression(depth + 1));
                } while (this.consume(','));
                this.expect(')');
            }
            return arguments;
        }

        private Argument expression(int depth) {
            if (depth > 40 || this.position >= this.text.length()) {
                throw invalid();
            }
            this.whitespace();
            if (this.position >= this.text.length()) {
                throw invalid();
            }
            char next = this.text.charAt(this.position);
            if (next == '\'' || next == '"') {
                this.string(next);
            } else if (next == '[') {
                this.position++;
                if (!this.consume(']')) {
                    do {
                        this.expression(depth + 1);
                    } while (this.consume(','));
                    this.expect(']');
                }
            } else if (Character.isDigit(next) || next == '-' || next == '+') {
                return this.number();
            } else {
                String name = this.identifier();
                this.whitespace();
                if ("__".equals(name)) {
                    this.expect('.');
                    return this.anonymousTraversal(this.identifier(), depth);
                } else if (STEPS.containsKey(name) && this.next('(')) {
                    return this.anonymousTraversal(name, depth);
                } else if ("P".equals(name) || "TextP".equals(name) || "Text".equals(name)) {
                    this.expect('.');
                    this.predicate(this.identifier(), depth,
                                   "Text".equals(name) ? TEXT_PREDICATES : PREDICATES);
                } else if (PREDICATES.contains(name)) {
                    this.predicate(name, depth, PREDICATES);
                } else {
                    if (this.consume('.')) {
                        name += "." + this.identifier();
                    }
                    if (!CONSTANTS.contains(name)) {
                        throw invalid();
                    }
                    return new Argument(name, null, null);
                }
            }
            return Argument.OTHER;
        }

        private Argument anonymousTraversal(String name, int depth) {
            if (!STEPS.containsKey(name)) {
                throw invalid();
            }
            List<Step> steps = new ArrayList<>();
            steps.add(new Step(name, this.arguments(depth), 0));
            this.chain(STEPS.keySet(), depth, steps);
            return new Argument(null, null, new Traversal(steps, this.position));
        }

        private void predicate(String name, int depth, Set<String> methods) {
            if (!methods.contains(name)) {
                throw invalid();
            }
            this.arguments(depth);
            this.chain(PREDICATES, depth, null);
        }

        private void string(char quote) {
            this.position++;
            while (this.position < this.text.length()) {
                char ch = this.text.charAt(this.position++);
                if (ch == quote) {
                    return;
                }
                if (ch == '\n' || ch == '\r' || ch == '$' && quote == '"') {
                    throw invalid();
                }
                if (ch == '\\') {
                    if (this.position >= this.text.length() ||
                        "\\'\"nrtbf".indexOf(this.text.charAt(this.position++)) < 0) {
                        throw invalid();
                    }
                }
            }
            throw invalid();
        }

        private Argument number() {
            int start = this.position;
            if (this.next('-') || this.next('+')) {
                this.position++;
            }
            int digitsStart = this.position;
            int radix = 10;
            boolean decimal = false;
            if (this.next('0') && this.position + 1 < this.text.length()) {
                char prefix = Character.toLowerCase(this.text.charAt(this.position + 1));
                if (prefix == 'x' || prefix == 'b') {
                    radix = prefix == 'x' ? 16 : 2;
                    this.position += 2;
                    digitsStart = this.position;
                }
            }
            this.digits(radix);
            int digitsEnd = this.position;
            if (radix == 10 && this.next('.')) {
                decimal = true;
                this.position++;
                this.digits(10);
            }
            if (radix == 10 && (this.next('e') || this.next('E'))) {
                decimal = true;
                this.position++;
                if (this.next('-') || this.next('+')) {
                    this.position++;
                }
                this.digits(10);
            }
            char suffix = 0;
            if (this.position < this.text.length()) {
                char next = Character.toLowerCase(this.text.charAt(this.position));
                if ("ilg".indexOf(next) >= 0 || radix == 10 && "df".indexOf(next) >= 0) {
                    suffix = next;
                    this.position++;
                }
            }
            if (decimal && (suffix == 'i' || suffix == 'l')) {
                throw invalid();
            }
            if (decimal || suffix == 'd' || suffix == 'f') {
                return Argument.OTHER;
            }
            String digits = this.text.substring(digitsStart, digitsEnd).replace("_", "");
            if (radix == 10 && digits.length() > 1 && digits.charAt(0) == '0') {
                radix = 8;
            }
            try {
                BigInteger value = new BigInteger(digits, radix);
                if (this.text.charAt(start) == '-') {
                    value = value.negate();
                }
                if (suffix == 'i') {
                    value.intValueExact();
                } else if (suffix == 'l') {
                    value.longValueExact();
                }
                return new Argument(null, value, null);
            } catch (NumberFormatException | ArithmeticException ignored) {
                throw invalid();
            }
        }

        private void digits(int radix) {
            int start = this.position;
            boolean lastDigit = false;
            while (this.position < this.text.length()) {
                char next = this.text.charAt(this.position);
                if (Character.digit(next, radix) >= 0 && next < 128) {
                    lastDigit = true;
                } else if (next == '_' && this.position > start) {
                    lastDigit = false;
                } else {
                    break;
                }
                this.position++;
            }
            if (start == this.position || !lastDigit) {
                throw invalid();
            }
        }

        private boolean next(char expected) {
            return this.position < this.text.length() && this.text.charAt(this.position) == expected;
        }

        private String identifier() {
            this.whitespace();
            int start = this.position;
            while (this.position < this.text.length()) {
                char ch = this.text.charAt(this.position);
                if (ch != '_' && !Character.isLetterOrDigit(ch)) {
                    break;
                }
                this.position++;
            }
            if (start == this.position) {
                throw invalid();
            }
            return this.text.substring(start, this.position);
        }

        private void expectIdentifier(String expected) {
            if (!expected.equals(this.identifier())) {
                throw invalid();
            }
        }

        private void expect(char expected) {
            if (!this.consume(expected)) {
                throw invalid();
            }
        }

        private boolean consume(char expected) {
            this.whitespace();
            if (this.position < this.text.length() && this.text.charAt(this.position) == expected) {
                this.position++;
                return true;
            }
            return false;
        }

        private void whitespace() {
            while (this.position < this.text.length() && Character.isWhitespace(this.text.charAt(this.position))) {
                this.position++;
            }
        }
    }
}
