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

// A convenience preflight for common reads, not a Gremlin parser or server safety boundary.
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

// Decode integer literals without evaluating user code. Groovy accepts radix prefixes,
// repeated interior underscores and I/L/G type suffixes; floating point is not a row bound.
const integerBound = argument => {
    const match = argument.trim().match(/^([+-]?)\s*(0[xX][\da-fA-F](?:[\da-fA-F_]*[\da-fA-F])?|0[bB][01](?:[01_]*[01])?|0(?:[0-7_]*[0-7])?|[1-9](?:[\d_]*\d)?)([iIlLgG]?)$/);
    if (!match) {
        return null;
    }
    let digits = match[2].replace(/_/g, '');
    if (/^0\d/.test(digits)) {
        digits = `0o${digits.slice(1)}`;
    }
    const value = BigInt(digits) * (match[1] === '-' ? -1n : 1n);
    const suffix = match[3].toLowerCase();
    if ((suffix === 'i' && (value < -2147483648n || value > 2147483647n))
        || value < 0n || value > 9223372036854775807n) {
        return null;
    }
    return value;
};

const hasLargeExplicitReadLimit = (text, language = 'gremlin', maximum = 1000) => {
    const code = maskLiterals(String(text ?? ''));
    if (code.includes(';')) {
        return false;
    }
    if (language.toLowerCase() === 'cypher') {
        if (!/^\s*(?:MATCH|OPTIONAL\s+MATCH)\b/i.test(code)
            || /\b(?:CREATE|MERGE|SET|DELETE|REMOVE|CALL)\b/i.test(code)) {
            return false;
        }
        const match = code.match(/\bLIMIT\s+(\d+)\s*$/i);
        return Boolean(match && Number(match[1]) > maximum);
    }
    if (!/^\s*g\s*\.\s*[VE]\s*\(/.test(code)
        || /\.\s*(?:count|sum|min|max|mean|fold|group|groupCount|aggregate|cap|addV|addE|property|drop|sideEffect|iterate)\s*\(/.test(code)) {
        return false;
    }
    let depth = 0;
    const limits = [];
    for (let i = 0; i < code.length; i++) {
        if (depth === 0 && code[i] === '.') {
            const match = code.slice(i).match(/^\.\s*limit\s*\((?:\s*(?:Scope\s*\.\s*)?global\s*,)?([^()]*)\)/);
            if (match) {
                const bound = integerBound(match[1]);
                if (bound !== null) {
                    limits.push(bound);
                }
            }
        }
        if (code[i] === '(') {
            depth++;
        }
        else if (code[i] === ')') {
            depth--;
        }
    }
    return limits.length > 0 && limits[limits.length - 1] > BigInt(maximum);
};

export {hasLargeExplicitReadLimit};
