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

import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';
import {fileURLToPath} from 'node:url';
import {build} from 'vite';

const require = createRequire(import.meta.url);
const helpers = require('../../hubble-dist/assembly/travis/graph_acceptance_helpers.js');
const frontend = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const repository = path.resolve(frontend, '../..');
const args = process.argv.slice(2);
const readOption = name => {
    const index = args.indexOf(name);
    if (index === -1) return undefined;
    assert.ok(args[index + 1] && !args[index + 1].startsWith('--'), `${name} requires a value`);
    return args[index + 1];
};
assert.ok(readOption('--output-dir'), 'Usage: node scripts/run-png-contract.mjs --output-dir DIR [--chromium-executable FILE]');
const output = path.resolve(readOption('--output-dir'));
fs.mkdirSync(output, {recursive: true});
const report = {status: 'failed', contract: 'Real public GraphController PNG; fixed world bounds, no projection or fitting',
    strictGate: {globalCoverage: .98, tileCoverage: .90, rgbDistance: 45, neighborRadius: 1, exclusions: []},
    cases: [], pageErrors: [], sourceFiles: {}, browserCleanup: 'not launched'};
const reportPath = path.join(output, 'png-contract.json');
const save = () => fs.writeFileSync(reportPath, JSON.stringify(report, null, 2) + '\n');
const hashFile = file => { report.sourceFiles[path.relative(repository, file)] = helpers.sha256(fs.readFileSync(file)); };
const bounded = async (promise, description) => {
    let timer;
    try {
        return await Promise.race([promise, new Promise((_, reject) => {
            timer = setTimeout(() => reject(new Error(`Timed out: ${description}`)), 30000);
        })]);
    } finally { clearTimeout(timer); }
};
const call = (target, method, ...args) => bounded(target[method](...args), `browser ${method}`);
const validateOpaqueBorder = (image, dpr) => {
    assert.equal(image.data.length, image.width * image.height * 4, 'PNG has no decoded pixels');
    const border = 30 * dpr;
    let foreground = 0;
    for (let y = 0; y < image.height; y++) for (let x = 0; x < image.width; x++) {
        const offset = (y * image.width + x) * 4;
        assert.equal(image.data[offset + 3], 255, `PNG is transparent at ${x},${y}`);
        const white = image.data[offset] === 255 && image.data[offset + 1] === 255 && image.data[offset + 2] === 255;
        if (x < border || y < border || x >= image.width - border || y >= image.height - border) {
            assert.ok(white, `PNG does not preserve its 30 CSS pixel white border at ${x},${y}`);
        }
        if (!white) foreground++;
    }
    assert.ok(foreground > 100, 'PNG contains no meaningful graph pixels');
    return {fullyOpaque: true, whiteBorderCssPixels: 30, foregroundPixels: foreground};
};

let browser;
save();
try {
    const bundled = await build({configFile: false, root: frontend, logLevel: 'warn',
        plugins: [{name: 'record-png-contract-inputs', generateBundle() {
            for (const id of this.getModuleIds()) {
                if (id.startsWith(`${frontend}/src/visualization/`) || id === path.join(frontend, 'scripts/png-contract-fixture.ts')) hashFile(id);
            }
        }}],
        build: {write: false, minify: false, sourcemap: false,
            lib: {entry: path.join(frontend, 'scripts/png-contract-fixture.ts'), name: 'PngContractFixture', formats: ['iife']}}});
    const outputs = (Array.isArray(bundled) ? bundled : [bundled]).flatMap(result => result.output);
    const chunks = outputs.filter(item => item.type === 'chunk');
    assert.equal(chunks.length, 1, 'Fixture must be one self-contained browser bundle');
    assert.deepEqual(chunks[0].imports, [], 'Fixture bundle must not request external browser modules');
    assert.deepEqual(chunks[0].dynamicImports, [], 'Fixture bundle must not request external browser modules');
    const bundle = chunks[0].code;
    report.bundleSha256 = helpers.sha256(Buffer.from(bundle));
    for (const file of [fileURLToPath(import.meta.url), path.join(frontend, 'package.json'),
        path.join(frontend, 'pnpm-lock.yaml'), path.join(repository, 'hugegraph-hubble/hubble-dist/assembly/travis/graph_acceptance_helpers.js')]) hashFile(file);
    for (const file of fs.readdirSync(path.join(frontend, 'patches'))) hashFile(path.join(frontend, 'patches', file));
    const {chromium} = helpers.loadPlaywright();
    browser = await chromium.launch({headless: true, timeout: 30000, executablePath: helpers.chromePath(readOption('--chromium-executable')
        || process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || process.env.CHROME_PATH || '')});
    report.browserVersion = browser.version();
    report.browserCleanup = 'pending';
    for (const name of ['circle-stroke', 'parallel-loop-fractional']) for (const dpr of [1, 2]) {
        const entry = {name, dpr, status: 'failed', phase: 'initial-render', negativeControls: []};
        report.cases.push(entry);
        let context, page;
        const prefix = path.join(output, `${name}-dpr${dpr}`);
        try {
            context = await call(browser, 'newContext', {viewport: {width: 700, height: 300}, deviceScaleFactor: dpr});
            page = await call(context, 'newPage');
            page.setDefaultTimeout(15000);
            page.on('pageerror', error => report.pageErrors.push({name, dpr, message: error.message}));
            const pixelPage = {evaluate: (...args) => call(page, 'evaluate', ...args)};
            // An intercepted document plus an in-memory source bundle needs no server.
            await call(page, 'route', 'http://png-contract.invalid/**', route => call(route, 'fulfill', {contentType: 'text/html',
                body: '<!doctype html><style>html,body{margin:0;background:white}#graph{width:700px;height:300px}</style><div id="graph"></div>'}));
            await call(page, 'goto', `http://png-contract.invalid/?case=${name}`);
            await call(page, 'addScriptTag', {content: bundle});
            entry.data = await bounded(call(page, 'evaluate', () => window.pngFixture.ready), 'fixture drawing');
            const exportImage = async suffix => {
                const result = await bounded(call(page, 'evaluate', () => window.pngFixture.exportPng()), 'public PNG export');
                assert.equal(result.status, 'applied', 'Public PNG export did not complete');
                assert.match(result.value, /^data:image\/png;base64,/, 'Public PNG export returned no PNG');
                const bytes = Buffer.from(result.value.split(',')[1], 'base64');
                assert.ok(bytes.length > 100, 'Public PNG export returned an empty artifact');
                fs.writeFileSync(`${prefix}-${suffix}.png`, bytes);
                return {bytes, image: await helpers.decodeScreenshot(pixelPage, bytes)};
            };
            const screenBytes = await call(page, 'screenshot', {path: `${prefix}-screen.png`});
            const screen = await helpers.decodeScreenshot(pixelPage, screenBytes);
            const original = await exportImage('export');
            entry.dimensions = {screen: {width: screen.width, height: screen.height},
                exported: {width: original.image.width, height: original.image.height}};
            assert.equal(original.image.width, 700 * dpr);
            assert.equal(original.image.height, 300 * dpr);
            entry.opacity = validateOpaqueBorder(original.image, dpr);
            entry.agreement = helpers.assertPngMatchesCanvas(screen, original.image);
            entry.exportSha256 = helpers.sha256(original.bytes);
            // Live style/hide state must reach the exported artifact; unchanged old
            // bytes must fail after either public command, then restoration must match.
            entry.phase = 'live-style';
            await bounded(call(page, 'evaluate', () => window.pngFixture.replace('changed-color')), 'live style update');
            const styledScreen = await helpers.decodeScreenshot(pixelPage, await call(page, 'screenshot', {path: `${prefix}-styled-screen.png`}));
            const styled = await exportImage('styled-export');
            entry.liveStyle = helpers.assertPngMatchesCanvas(styledScreen, styled.image);
            assert.throws(() => helpers.assertPngMatchesCanvas(styledScreen, original.image), undefined, 'Stale export escaped strict PNG gate');
            entry.negativeControls.push({name: 'stale-export', rejected: true, actualPublicGraph: true});
            await bounded(call(page, 'evaluate', () => window.pngFixture.replace()), 'restore fixture style');
            entry.phase = 'live-edge-style';
            await bounded(call(page, 'evaluate', () => window.pngFixture.replace('edge-style')), 'live edge stroke update');
            const edgeScreen = await helpers.decodeScreenshot(pixelPage, await call(page, 'screenshot', {path: `${prefix}-edge-style-screen.png`}));
            const edgeStyled = await exportImage('edge-style-export');
            entry.liveEdgeStyle = helpers.assertPngMatchesCanvas(edgeScreen, edgeStyled.image);
            assert.throws(() => helpers.assertPngMatchesCanvas(edgeScreen, original.image), undefined,
                'Stale edge and arrow appearance escaped strict PNG gate');
            entry.negativeControls.push({name: 'pre-edge-style-export', rejected: true, actualPublicGraph: true});
            await bounded(call(page, 'evaluate', () => window.pngFixture.replace()), 'restore fixture edge style');
            entry.phase = 'live-hide';
            const hiddenResult = await bounded(call(page, 'evaluate', () => window.pngFixture.hideMiddle(true)), 'hide interior node');
            assert.equal(hiddenResult.status, 'applied');
            const hiddenScreen = await helpers.decodeScreenshot(pixelPage, await call(page, 'screenshot', {path: `${prefix}-hidden-screen.png`}));
            const hidden = await exportImage('hidden-export');
            entry.liveHide = helpers.assertPngMatchesCanvas(hiddenScreen, hidden.image);
            assert.throws(() => helpers.assertPngMatchesCanvas(hiddenScreen, original.image), undefined, 'Visible old export escaped hidden graph gate');
            entry.negativeControls.push({name: 'pre-hide-export', rejected: true, actualPublicGraph: true});
            const shownResult = await bounded(call(page, 'evaluate', () => window.pngFixture.hideMiddle(false)), 'restore interior node');
            assert.equal(shownResult.status, 'applied');
            helpers.assertPngMatchesCanvas(screen, (await exportImage('restored-export')).image);
            // Actual pointer pan makes part of the graph leave the viewport. Full
            // PNG must still retain exactly the same world data and raster bytes.
            entry.phase = 'camera-independence';
            await call(page.mouse, 'move', 8, 150);
            await call(page.mouse, 'down', );
            await call(page.mouse, 'move', 208, 230, {steps: 12});
            await call(page.mouse, 'up', );
            await call(page, 'evaluate', () => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
            const panned = await call(page, 'screenshot', {path: `${prefix}-panned.png`});
            assert.notEqual(helpers.sha256(panned), helpers.sha256(screenBytes), 'Browser pan changed no graph pixels');
            assert.equal(helpers.sha256((await exportImage('after-pan')).bytes), entry.exportSha256, 'Full PNG changed after viewport pan');
            const zoom = await bounded(call(page, 'evaluate', () => window.pngFixture.zoom()), 'public camera zoom');
            assert.equal(zoom.status, 'applied');
            assert.equal(helpers.sha256((await exportImage('after-zoom')).bytes), entry.exportSha256, 'Full PNG changed after viewport zoom');
            entry.cameraIndependence = {realPointerPan: true, publicZoom: true, identicalFullPng: true};

            // Real graph variants establish that the strict gate rejects missing
            // content, rather than merely matching dimensions or one filled area.
            // These variants retain the fixed outer anchors.
            entry.phase = 'negative-controls';
            if (name === 'circle-stroke') {
                for (const mutation of ['missing-node', 'missing-edge']) {
                    await bounded(call(page, 'evaluate', value => window.pngFixture.replace(value), mutation), mutation);
                    const changed = await exportImage(mutation);
                    assert.throws(() => helpers.assertPngMatchesCanvas(screen, changed.image), undefined, `${mutation} escaped strict PNG gate`);
                    entry.negativeControls.push({name: mutation, rejected: true, actualPublicGraph: true});
                }
                const cropped = {width: original.image.width - 10, height: original.image.height, data: original.image.data};
                assert.throws(() => helpers.assertPngMatchesCanvas(screen, cropped));
                entry.negativeControls.push({name: 'cropped-export', rejected: true});
                const blank = {...original.image, data: new Uint8ClampedArray(original.image.data.length).fill(255)};
                assert.throws(() => helpers.assertPngMatchesCanvas(screen, blank));
                entry.negativeControls.push({name: 'blank-export', rejected: true});
                const transparent = {...original.image, data: new Uint8ClampedArray(original.image.data)};
                transparent.data[3] = 0;
                assert.throws(() => validateOpaqueBorder(transparent, dpr), /transparent/);
                entry.negativeControls.push({name: 'transparent-export', rejected: true});
            }
            entry.phase = 'empty-export';
            const cleared = await bounded(call(page, 'evaluate', () => window.pngFixture.clear()), 'empty graph');
            assert.equal(cleared.status, 'applied');
            const empty = await exportImage('empty-export');
            assert.equal(empty.image.width, 60 * dpr + 1, 'Empty PNG must retain one device pixel plus padding');
            assert.equal(empty.image.height, 60 * dpr + 1, 'Empty PNG must retain one device pixel plus padding');
            assert.ok(empty.image.data.every(channel => channel === 255), 'Empty PNG is not fully opaque white');
            entry.emptyGraph = {width: empty.image.width, height: empty.image.height, opaqueWhite: true};
            entry.phase = 'complete';
            entry.status = 'passed';
        } catch (error) { entry.error = {name: error.name, message: error.message}; }
        finally {
            if (page) await bounded(call(page, 'evaluate', () => window.pngFixture?.dispose()), 'fixture disposal').catch(error => {
                entry.status = 'failed'; entry.cleanupError = error.message;
            });
            if (context) await bounded(context.close(), 'context cleanup').catch(error => {
                entry.status = 'failed';
                entry.contextCleanupError = error.message;
            });
            save();
        }
        if (!context || entry.contextCleanupError) {
            throw new Error('Browser context unavailable after case; stopped before creating another context');
        }
    }
    assert.equal(report.cases.length, 4, 'Required real PNG cases were not executed');
    assert.deepEqual(report.pageErrors, [], 'Unexpected browser errors');
    assert.ok(report.cases.every(entry => entry.status === 'passed'), 'A real PNG case failed; see png-contract.json');
    report.status = 'passed';
} catch (error) { report.error = {name: error.name, message: error.message}; process.exitCode = 1; }
finally {
    if (browser) {
        try {
            await bounded(browser.close(), 'browser cleanup');
            report.browserCleanup = 'own browser closed';
        } catch (error) {
            report.browserCleanup = {status: 'failed', error: error.message};
            report.status = 'failed';
            process.exitCode = 1;
        }
    }
    save();
    console.log(JSON.stringify({status: report.status, cases: report.cases.map(({name, dpr, status, error}) => ({name, dpr, status, error})), report: reportPath}));
}
