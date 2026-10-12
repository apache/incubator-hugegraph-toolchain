#!/usr/bin/env node

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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const fs = require('fs');
const path = require('path');

const projectRoot = path.resolve(__dirname, '..');
const resourcesRoot = path.join(projectRoot, 'src', 'i18n', 'resources');
const sourceRoot = path.join(projectRoot, 'src');
const locales = ['zh-CN', 'en-US'];
const ignoredSourceDirs = new Set(['i18n', 'node_modules']);
const problems = [];
const sharedConstants = loadSharedConstants();

if (process.argv.includes('--self-test')) {
  runSelfTests();
  process.exit(0);
}

const localeData = Object.fromEntries(
  locales.map((locale) => [locale, loadLocale(locale)])
);

checkSymmetricKeys();
checkLocaleValues();
checkStaticTranslationUsage();

if (problems.length > 0) {
  console.error(`i18n check failed with ${problems.length} issue(s):`);
  for (const problem of problems) {
    console.error(`- ${problem}`);
  }
  process.exit(1);
}

console.log(JSON.stringify({
  status: 'passed',
  locales: Object.fromEntries(
    locales.map((locale) => [locale, Object.keys(localeData[locale].flat).length])
  ),
  checkedStaticKeys: collectStaticTranslationKeys().length
}, null, 2));

function loadLocale(locale) {
  const root = path.join(resourcesRoot, locale);
  const files = listFiles(root, (file) => file.endsWith('.json'));
  const byFile = new Map();
  const merged = {};

  for (const file of files) {
    const relativePath = path.relative(root, file);
    const json = JSON.parse(fs.readFileSync(file, 'utf8'));
    byFile.set(relativePath, flatten(json));
  }

  for (const moduleFile of getMergedModuleFiles(locale)) {
    const file = path.join(root, moduleFile);
    if (fs.existsSync(file)) {
      deepMerge(merged, JSON.parse(fs.readFileSync(file, 'utf8')));
    }
  }

  return {
    byFile,
    flat: flatten(Object.fromEntries(
      Array.from(byFile.entries()).map(([file, flat]) => [file, flat])
    )),
    merged,
    mergedFlat: flatten(merged)
  };
}

function getMergedModuleFiles(locale) {
  const indexPath = path.join(resourcesRoot, locale, 'index.js');
  const indexText = fs.readFileSync(indexPath, 'utf8');
  const imports = new Map();

  for (const barrelImport of getNamedImports(indexText)) {
    const barrel = path.join(resourcesRoot, locale, barrelImport.source, 'index.js');
    if (!fs.existsSync(barrel)) {
      continue;
    }
    const barrelText = fs.readFileSync(barrel, 'utf8');
    const barrelImports = getDefaultJsonImports(barrelText, barrelImport.source);
    for (const name of barrelImport.names) {
      if (barrelImports.has(name)) {
        imports.set(name, barrelImports.get(name));
      }
    }
  }

  for (const [name, file] of getDefaultJsonImports(indexText)) {
    imports.set(name, file);
  }

  return getMergeArguments(indexText)
    .map((name) => imports.get(name))
    .filter(Boolean);
}

function getDefaultJsonImports(text, parentDir = '') {
  const imports = new Map();
  const importPattern = /import\s+(\w+)\s+from\s+(['"])\.\/([^'"]+\.json)\2/g;
  let match;

  while ((match = importPattern.exec(text)) !== null) {
    imports.set(match[1], path.join(parentDir, match[3]));
  }
  return imports;
}

function getNamedImports(text) {
  const imports = [];
  const importPattern = /import\s+\{([^}]*)\}\s+from\s+(['"])\.\/([^'"]+)\2;?/g;
  let match;

  while ((match = importPattern.exec(text)) !== null) {
    imports.push({
      names: match[1].split(',').map((name) => name.trim()).filter(Boolean),
      source: match[3]
    });
  }
  return imports;
}

function getMergeArguments(indexText) {
  const start = indexText.indexOf('merge(');
  if (start === -1) {
    return [];
  }
  let depth = 0;
  let end = start;
  for (; end < indexText.length; end++) {
    if (indexText[end] === '(') {
      depth++;
    } else if (indexText[end] === ')') {
      depth--;
      if (depth === 0) {
        break;
      }
    }
  }
  return indexText
    .slice(start + 'merge('.length, end)
    .split(',')
    .map((name) => name.trim())
    .filter(Boolean);
}

function checkSymmetricKeys() {
  const zhFiles = localeData['zh-CN'].byFile;
  const enFiles = localeData['en-US'].byFile;
  const allFiles = new Set([...zhFiles.keys(), ...enFiles.keys()]);

  for (const file of allFiles) {
    if (!zhFiles.has(file)) {
      problems.push(`zh-CN missing resource file ${file}`);
      continue;
    }
    if (!enFiles.has(file)) {
      problems.push(`en-US missing resource file ${file}`);
      continue;
    }
    const zhKeys = new Set(Object.keys(zhFiles.get(file)));
    const enKeys = new Set(Object.keys(enFiles.get(file)));
    for (const key of zhKeys) {
      if (!enKeys.has(key)) {
        problems.push(`en-US missing key ${file}:${key}`);
      }
    }
    for (const key of enKeys) {
      if (!zhKeys.has(key)) {
        problems.push(`zh-CN missing key ${file}:${key}`);
      }
    }
  }
}

function checkLocaleValues() {
  for (const locale of locales) {
    for (const [file, entries] of localeData[locale].byFile.entries()) {
      for (const [key, value] of Object.entries(entries)) {
        if (typeof value !== 'string') {
          continue;
        }
        if (value.trim() === '') {
          problems.push(`${locale} empty value ${file}:${key}`);
        }
        if (
          locale === 'en-US' &&
          /[\u4e00-\u9fff]/.test(value)
        ) {
          problems.push(`${locale} Chinese text ${file}:${key}=${value}`);
        }
        if (locale === 'en-US' && /[，。！？；：（）【】《》]/.test(value)) {
          problems.push(`${locale} full-width punctuation ${file}:${key}=${value}`);
        }
        if (/\b(TODO|TBD|xxx)\b/i.test(key) || /\b(TODO|TBD|xxx)\b/i.test(value)) {
          problems.push(`${locale} placeholder ${file}:${key}=${value}`);
        }
        if (looksLikeRawKey(value) && value === key) {
          problems.push(`${locale} raw key value ${file}:${key}`);
        }
      }
    }
  }
}

function checkStaticTranslationUsage() {
  const staticKeys = collectStaticTranslationKeys();
  const zhMerged = localeData['zh-CN'].mergedFlat;
  const enMerged = localeData['en-US'].mergedFlat;

  for (const key of staticKeys) {
    if (!Object.prototype.hasOwnProperty.call(zhMerged, key)) {
      problems.push(`zh-CN missing merged static t() key ${key}`);
    }
    if (!Object.prototype.hasOwnProperty.call(enMerged, key)) {
      problems.push(`en-US missing merged static t() key ${key}`);
    }
  }
}

function collectStaticTranslationKeys() {
  const keys = new Set();
  for (const file of listFiles(sourceRoot, isJavaScriptLikeFile)) {
    if (shouldIgnoreSource(file)) {
      continue;
    }
    const content = fs.readFileSync(file, 'utf8');
    for (const key of extractStaticTranslationKeys(content, sharedConstants)) {
      keys.add(key);
    }
  }
  return Array.from(keys).sort();
}

function extractStaticTranslationKeys(content, baseConstants = new Map()) {
  const keys = [];
  const constants = collectStringConstants(content, baseConstants);
  const source = stripComments(content);
  const translationPattern = /\bt\(\s*([^)]+)\)/g;
  let match;

  while ((match = translationPattern.exec(source)) !== null) {
    const key = resolveStringExpression(match[1].trim(), constants);
    if (key) {
      keys.push(key);
    }
  }
  return keys;
}

function loadSharedConstants() {
  const constants = new Map();
  const constantsPath = path.join(sourceRoot, 'utils', 'constants.js');
  if (!fs.existsSync(constantsPath)) {
    return constants;
  }

  const content = fs.readFileSync(constantsPath, 'utf8');
  const textPathMatch = content.match(/export\s+const\s+TEXT_PATH\s*=\s*\{([\s\S]*?)\};/);
  if (!textPathMatch) {
    return constants;
  }

  const entryPattern = /(\w+)\s*:\s*(['"])((?:\\[\s\S]|(?!\2)[\s\S])*?)\2/g;
  let match;
  while ((match = entryPattern.exec(textPathMatch[1])) !== null) {
    constants.set(`TEXT_PATH.${match[1]}`, unescapeQuotedString(match[3]));
  }
  return constants;
}

function collectStringConstants(content, baseConstants) {
  const constants = new Map(baseConstants);
  const source = stripComments(content);
  const declarationPattern = /\bconst\s+(\w+)\s*=\s*([^;]+);/g;
  const declarations = [];
  let match;

  while ((match = declarationPattern.exec(source)) !== null) {
    declarations.push([match[1], match[2].trim()]);
  }

  let changed = true;
  while (changed) {
    changed = false;
    for (const [name, expression] of declarations) {
      if (constants.has(name)) {
        continue;
      }
      const value = resolveStringExpression(expression, constants);
      if (value) {
        constants.set(name, value);
        changed = true;
      }
    }
  }
  return constants;
}

function resolveStringExpression(expression, constants) {
  const value = expression.trim();
  if (!value) {
    return null;
  }

  const literal = parseQuotedLiteral(value);
  if (literal !== null) {
    return literal;
  }

  if (constants.has(value)) {
    return constants.get(value);
  }

  const template = resolveTemplateLiteral(value, constants);
  if (template !== null) {
    return template;
  }

  const parts = splitTopLevelConcat(value);
  if (parts.length > 1) {
    const resolved = parts.map((part) => resolveStringExpression(part, constants));
    if (resolved.every((part) => part !== null)) {
      return resolved.join('');
    }
  }

  return null;
}

function resolveTemplateLiteral(expression, constants) {
  if (!expression.startsWith('`') || !expression.endsWith('`')) {
    return null;
  }
  const body = expression.slice(1, -1);
  let unresolved = false;
  const resolved = body.replace(/\$\{([^}]+)\}/g, (fullMatch, name) => {
    const value = resolveStringExpression(name, constants);
    if (value === null) {
      unresolved = true;
      return fullMatch;
    }
    return value;
  });
  return unresolved ? null : unescapeQuotedString(resolved);
}

function splitTopLevelConcat(expression) {
  const parts = [];
  let quote = null;
  let depth = 0;
  let start = 0;
  for (let index = 0; index < expression.length; index++) {
    const char = expression[index];
    const prev = expression[index - 1];
    if (quote) {
      if (char === quote && prev !== '\\') {
        quote = null;
      }
      continue;
    }
    if (char === '\'' || char === '"' || char === '`') {
      quote = char;
      continue;
    }
    if (char === '(') {
      depth++;
    } else if (char === ')') {
      depth--;
    } else if (char === '+' && depth === 0) {
      parts.push(expression.slice(start, index).trim());
      start = index + 1;
    }
  }
  parts.push(expression.slice(start).trim());
  return parts;
}

function parseQuotedLiteral(expression) {
  if (expression.length < 2) {
    return null;
  }
  const quote = expression[0];
  if ((quote !== '\'' && quote !== '"') || expression[expression.length - 1] !== quote) {
    return null;
  }
  return unescapeQuotedString(expression.slice(1, -1));
}

function unescapeQuotedString(value) {
  return value.replace(/\\(['"`\\])/g, '$1');
}

function stripComments(content) {
  let output = '';
  let quote = null;
  let lineComment = false;
  let blockComment = false;

  for (let index = 0; index < content.length; index++) {
    const char = content[index];
    const next = content[index + 1];
    const prev = content[index - 1];

    if (lineComment) {
      if (char === '\n') {
        lineComment = false;
        output += char;
      } else {
        output += ' ';
      }
      continue;
    }

    if (blockComment) {
      if (char === '*' && next === '/') {
        blockComment = false;
        output += '  ';
        index++;
      } else {
        output += char === '\n' ? '\n' : ' ';
      }
      continue;
    }

    if (quote) {
      output += char;
      if (char === quote && prev !== '\\') {
        quote = null;
      }
      continue;
    }

    if (char === '\'' || char === '"' || char === '`') {
      quote = char;
      output += char;
    } else if (char === '/' && next === '/') {
      lineComment = true;
      output += '  ';
      index++;
    } else if (char === '/' && next === '*') {
      blockComment = true;
      output += '  ';
      index++;
    } else {
      output += char;
    }
  }
  return output;
}

function shouldIgnoreSource(file) {
  const relative = path.relative(sourceRoot, file);
  return relative.split(path.sep).some((segment) => ignoredSourceDirs.has(segment));
}

function isJavaScriptLikeFile(file) {
  return /\.(js|jsx|ts|tsx)$/.test(file);
}

function looksLikeRawKey(value) {
  return /^[A-Za-z][A-Za-z0-9_-]*(\.[A-Za-z0-9_-]+)+$/.test(value);
}

function listFiles(root, predicate) {
  const out = [];
  for (const entry of fs.readdirSync(root, {withFileTypes: true})) {
    const file = path.join(root, entry.name);
    if (entry.isDirectory()) {
      out.push(...listFiles(file, predicate));
    } else if (predicate(file)) {
      out.push(file);
    }
  }
  return out;
}

function flatten(obj, prefix = '', out = {}) {
  for (const [key, value] of Object.entries(obj)) {
    const nextKey = prefix ? `${prefix}.${key}` : key;
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      flatten(value, nextKey, out);
    } else {
      out[nextKey] = value;
    }
  }
  return out;
}

function deepMerge(target, source) {
  for (const [key, value] of Object.entries(source)) {
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      if (!target[key] || typeof target[key] !== 'object' || Array.isArray(target[key])) {
        target[key] = {};
      }
      deepMerge(target[key], value);
    } else {
      target[key] = value;
    }
  }
  return target;
}

function runSelfTests() {
  const defaultImports = getDefaultJsonImports(
    'import common from "./common.json";\nimport home from \'./modules/home.json\';'
  );
  assert(defaultImports.get('common') === 'common.json', 'double-quoted JSON import');
  assert(defaultImports.get('home') === path.join('modules', 'home.json'), 'single import');

  const namedImports = getNamedImports('import {\n  common,\n  modules\n} from "./modules";');
  assert(namedImports.length === 1, 'multi-line named import');
  assert(namedImports[0].names.join(',') === 'common,modules', 'named import names');
  assert(namedImports[0].source === 'modules', 'double-quoted named import source');

  const staticKeys = extractStaticTranslationKeys(
    't("addition.save"); // t("missing.comment")\n' +
    't(\'common.cancel\'); t(`dynamic.${id}`); t("quote.\\"key");\n' +
    'const BASE = "analysis.algorithm.olap.item"; t(`${BASE}.PAGE_RANK`);\n' +
    'const OWNED_TEXT_PATH = TEXT_PATH.OLAP + ".page_rank"; t(OWNED_TEXT_PATH + ".desc");',
    new Map([
      ['TEXT_PATH.OLAP', 'analysis.algorithm.olap']
    ])
  );
  assert(staticKeys.includes('addition.save'), 'double-quoted static t()');
  assert(staticKeys.includes('common.cancel'), 'single-quoted static t()');
  assert(staticKeys.includes('quote."key'), 'escaped quote in static t()');
  assert(staticKeys.includes('analysis.algorithm.olap.item.PAGE_RANK'), 'template constant t()');
  assert(staticKeys.includes('analysis.algorithm.olap.page_rank.desc'), 'concatenated t()');
  assert(!staticKeys.includes('missing.comment'), 'commented t() ignored');
  assert(!staticKeys.some((key) => key.includes('${')), 'dynamic template key ignored');

  console.log(JSON.stringify({status: 'passed', selfTests: 13}, null, 2));
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(`self-test failed: ${message}`);
  }
}
