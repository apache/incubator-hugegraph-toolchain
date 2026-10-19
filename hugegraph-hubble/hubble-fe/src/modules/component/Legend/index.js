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

/**
 * @file  Legend
 */

import {useContext, useMemo, useEffect} from 'react';
import {useTranslation} from 'react-i18next';
import {GraphContext} from '../Context';

const Legend = () => {
    const {t} = useTranslation();
    const {controller, snapshot, run, showLegend = true} = useContext(GraphContext);
    const groups = useMemo(() => {
        const result = [];
        for (const kind of ['nodes', 'edges']) {
            const labels = new Map();
            for (const element of snapshot?.data[kind] || []) {
                const label = element.data.legend;
                if (label === undefined) continue;
                if (!labels.has(label)) labels.set(label, []);
                labels.get(label).push(element);
            }
            labels.forEach((elements, label) => result.push({kind, label, elements}));
        }
        return result;
    }, [snapshot?.data]);
    useEffect(() => {
        if (!controller || !showLegend) return undefined;
        return () => { run(() => controller.highlight([])); };
    }, [controller, run, showLegend]);
    if (!showLegend || !groups.length) return null;
    const ready = Boolean(controller && snapshot?.status === 'ready');
    return (
        <div aria-label={t('analysis.canvas.legend.title')} style={{position: 'absolute', left: 12, top: 8,
            maxWidth: 'calc(100% - 24px)', display: 'flex', gap: 6, flexWrap: 'wrap', zIndex: 2}}>
            <span>{t('analysis.canvas.legend.title')}</span>
            {groups.map(({kind, label, elements}) => {
                const ids = elements.map(element => element.id);
                const hidden = elements.every(element => element.style?.visibility === 'hidden');
                const color = elements[0].style?.stroke || elements[0].style?.fill || '#667085';
                return (
                    <button key={`${kind}:${label}`} type="button" disabled={!ready}
                        aria-pressed={!hidden}
                        onPointerEnter={() => { if (ready) run(() => controller.highlight(ids)); }}
                        onPointerLeave={() => { if (ready) run(() => controller.highlight([])); }}
                        onFocus={() => { if (ready) run(() => controller.highlight(ids)); }}
                        onBlur={() => { if (ready) run(() => controller.highlight([])); }}
                        onClick={() => run(() => controller.hide(ids, !hidden))}
                        style={{border: '1px solid #d9d9d9', borderRadius: 4, background: '#fff',
                            padding: '2px 7px', opacity: hidden ? 0.5 : 1, cursor: 'pointer'}}>
                        <span aria-hidden="true" style={{color, marginRight: 4}}>{kind === 'nodes' ? '●' : '━'}</span>
                        {label}
                    </button>
                );
            })}
        </div>
    );
};

export default Legend;
