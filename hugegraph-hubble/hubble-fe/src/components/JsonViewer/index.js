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

import {useState} from 'react';
import {Typography} from 'antd';
import JSONbig from 'json-bigint';
import './index.scss';

const json = value => JSONbig.stringify(value, null, 2) ?? String(value);

const JsonBranch = ({value, label, depth = 0}) => {
    const [open, setOpen] = useState(true);
    const collection = value !== null && typeof value === 'object' && value._isBigNumber !== true;
    if (!collection) {
        return <div className='json-viewer-leaf'>
            {label !== undefined && <span className='json-viewer-key'>{json(label)}: </span>}
            <span>{json(value)}</span>
        </div>;
    }
    const array = Array.isArray(value);
    const entries = open ? Object.entries(value) : [];
    const grouped = array && entries.length > 50;
    return (
        <div className='json-viewer-branch'>
            <details open={open} onToggle={event => setOpen(event.currentTarget.open)}>
                <summary>
                    {label !== undefined && <span className='json-viewer-key'>{json(label)}: </span>}
                    {array ? '[' : '{'}{!open && `…${array ? ']' : '}'}`}
                </summary>
                {open && <div className='json-viewer-children'>
                    {grouped ? Array.from({length: Math.ceil(entries.length / 50)}, (_, group) => (
                        <ArrayGroup key={group} entries={entries.slice(group * 50, (group + 1) * 50)}
                            start={group * 50} depth={depth + 1} />
                    )) : entries.map(([key, item]) => (
                        <JsonBranch key={key} label={array ? Number(key) : key} value={item} depth={depth + 1} />
                    ))}
                </div>}
                {open && <span>{array ? ']' : '}'}</span>}
            </details>
            <Typography.Text className='json-viewer-copy' copyable={{text: () => json(value)}} />
        </div>
    );
};

const ArrayGroup = ({entries, start, depth}) => {
    const [open, setOpen] = useState(false);
    return <details onToggle={event => setOpen(event.currentTarget.open)}>
        <summary>[{start} … {start + entries.length - 1}]</summary>
        {open && entries.map(([key, item]) => (
            <JsonBranch key={key} label={Number(key)} value={item} depth={depth} />
        ))}
    </details>;
};

// Read-only JSON presentation. React escapes all keys and values; clipboard access occurs only on user action.
const JsonViewer = ({value}) => (
    <div className='json-viewer'><JsonBranch value={value} /></div>
);

export default JsonViewer;
