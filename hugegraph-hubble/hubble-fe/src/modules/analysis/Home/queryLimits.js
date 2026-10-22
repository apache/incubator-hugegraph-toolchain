/*
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

import {GRAPH_NODE_LIMIT} from '../QueryResult/Home/utils';

// Mask quoted text and comments before the small, literal-only convenience check.
const maskLiterals = text => {
    let output = '';
    let quote = '';
    let comment = '';
    for (let i = 0; i < text.length; i++) {
        const ch = text[i];
        const next = text[i + 1];
        if (comment) {
            output += ' ';
            if (comment === 'line' && ch === '\n') {
                comment = '';
            }
            else if (comment === 'block' && ch === '*' && next === '/') {
                output += ' ';
                i++;
                comment = '';
            }
        }
        else if (quote) {
            output += ' ';
            if (ch === '\\') {
                output += ' ';
                i++;
            }
            else if (ch === quote) {
                quote = '';
            }
        }
        else if (ch === "'" || ch === '"' || ch === '`') {
            quote = ch;
            output += ' ';
        }
        else if (ch === '/' && (next === '/' || next === '*')) {
            comment = next === '/' ? 'line' : 'block';
            output += '  ';
            i++;
        }
        else {
            output += ch;
        }
    }
    return quote || comment === 'block' ? '' : output.trim().replace(/;\s*$/, '');
};

// This only recognizes a common terminal decimal limit, without inferring result
// cardinality or evaluating expressions. Unmatched manual scripts go unchanged to
// the backend/render preview budgets; this does not guarantee bounded fetching.
const hasLargeExplicitReadLimit = (text, language = 'gremlin', maximum = GRAPH_NODE_LIMIT) => {
    const code = maskLiterals(String(text ?? ''));
    if (code.includes(';')) {
        return false;
    }
    let match;
    if (language.toLowerCase() === 'cypher') {
        // Dotted identifiers are properties, and a WITH input limit before RETURN
        // is not the final result-limit spelling recognized here.
        const clauses = code.replace(/\.\s*(?:RETURN|LIMIT)\b/gi, '.');
        match = clauses.match(/(?:^|[^\w$])RETURN\b[\s\S]*\bLIMIT\s+(0|[1-9]\d*)\s*$/i);
    }
    else if (/^g\s*\.\s*[VE]\s*\(/.test(code)) {
        match = code.match(/\.\s*limit\s*\(\s*(0|[1-9]\d*)\s*\)\s*$/);
    }
    return Boolean(match && Number(match[1]) > maximum);
};

export {hasLargeExplicitReadLimit};
