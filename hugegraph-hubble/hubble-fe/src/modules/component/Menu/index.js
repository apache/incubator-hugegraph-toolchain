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

import {useContext, useEffect, useLayoutEffect, useRef, useState} from 'react';
import {useTranslation} from 'react-i18next';
import {GraphContext} from '../Context';

const Menu = ({onClickAddNode, onClickAddEdge, onClickExpand, onClickSearch}) => {
    const {t} = useTranslation();
    const {controller, snapshot, containerRef, run, contextTarget: target, setContextTarget} = useContext(GraphContext);
    const menuRef = useRef(null);
    const [position, setPosition] = useState({left: 0, top: 0});
    useLayoutEffect(() => {
        const container = containerRef?.current;
        const menu = menuRef.current;
        if (!target || !container || !menu) return;
        const bounds = container.getBoundingClientRect();
        setPosition({
            left: Math.max(0, Math.min((target.client?.x ?? bounds.left) - bounds.left,
                Math.max(0, bounds.width - menu.offsetWidth))),
            top: Math.max(0, Math.min((target.client?.y ?? bounds.top) - bounds.top,
                Math.max(0, bounds.height - menu.offsetHeight))),
        });
        menu.querySelector('button')?.focus();
    }, [target, containerRef]);
    useEffect(() => {
        if (!target) return undefined;
        const dismiss = event => {
            if (event.type === 'keydown' ? event.key === 'Escape' : !menuRef.current?.contains(event.target)) {
                setContextTarget(null);
            }
        };
        // Dismiss the old menu before G6 emits a new one from pointerdown.
        document.addEventListener('pointerdown', dismiss, true);
        document.addEventListener('keydown', dismiss);
        return () => {
            document.removeEventListener('pointerdown', dismiss, true);
            document.removeEventListener('keydown', dismiss);
        };
    }, [target, setContextTarget]);
    if (!target || !controller || snapshot?.status !== 'ready') return null;
    const actions = [];
    const add = (key, operation) => actions.push({key, operation});
    const raw = target.data?.raw;
    if (target.kind === 'node' && raw) {
        const info = {vertexId: raw.id, vertexLabel: raw.label};
        const fixed = snapshot.data.nodes.find(node => node.id === target.id)?.data.fixed;
        if (onClickExpand) add('expand', () => onClickExpand({vertex_id: raw.id, vertex_label: raw.label}));
        if (onClickSearch) add('search', () => onClickSearch(info));
        add(fixed ? 'unfix' : 'fix', () => controller.pin([target.id], !fixed));
        add('hide', () => controller.hide([target.id], true));
        if (onClickAddEdge) {
            add('add_out_edge', () => onClickAddEdge(info, true));
            add('add_in_edge', () => onClickAddEdge(info, false));
        }
    }
    else if (target.kind === 'edge') add('hide', () => controller.hide([target.id], true));
    else if (target.kind === 'canvas' && onClickAddNode) add('add_vertex', () => onClickAddNode());
    if (!actions.length) return null;
    return (
        <div ref={menuRef} role="menu" style={{position: 'absolute', ...position, zIndex: 20,
            minWidth: 150, maxWidth: '100%', maxHeight: '100%', overflow: 'auto', padding: 4,
            background: '#fff', border: '1px solid #d9d9d9', borderRadius: 4,
            boxShadow: '0 3px 12px #0002', display: 'flex', flexDirection: 'column'}}>
            {actions.map(({key, operation}) => (
                <button key={key} type="button" role="menuitem"
                    style={{padding: '7px 12px', border: 0, textAlign: 'left', background: 'transparent',
                        color: '#262626', cursor: 'pointer'}}
                    onClick={() => { setContextTarget(null); run(operation); }}>
                    {t(`analysis.canvas.context_menu.${key}`)}
                </button>
            ))}
        </div>
    );
};

export default Menu;
