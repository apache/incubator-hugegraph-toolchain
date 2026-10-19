/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {act, fireEvent, screen, waitFor} from '@testing-library/react';
import {useLocation, useNavigate} from 'react-router-dom';
import ReactDOM from 'react-dom/client';

jest.mock('react-dom/client', () => {
    const actual = jest.requireActual('react-dom/client');
    return {...actual, createRoot: jest.fn(actual.createRoot)};
});
jest.mock('./App', () => function RouterProbe() {
    const location = useLocation();
    const navigate = useNavigate();
    return <>
        <output role="status">{location.pathname}{location.search}{location.hash}</output>
        <button onClick={() => navigate('/graphspace/DEFAULT/graph/hugegraph/meta')}>Schema</button>
    </>;
});

test('production entry preserves deep links and navigates through the real router', async () => {
    const container = document.createElement('div');
    container.id = 'root';
    document.body.append(container);
    window.history.replaceState({}, '', '/gremlin/DEFAULT/hugegraph?x=1#result');
    let root;
    try {
        await act(async () => { await import('./index'); });
        root = ReactDOM.createRoot.mock.results.at(-1).value;
        expect(screen.getByRole('status')).toHaveTextContent('/gremlin/DEFAULT/hugegraph?x=1#result');
        fireEvent.click(screen.getByRole('button', {name: 'Schema'}));
        await waitFor(() => expect(screen.getByRole('status'))
            .toHaveTextContent('/graphspace/DEFAULT/graph/hugegraph/meta'));
        expect(window.location.pathname).toBe('/graphspace/DEFAULT/graph/hugegraph/meta');
    }
    finally {
        if (root) await act(async () => root.unmount());
        container.remove();
        window.history.replaceState({}, '', '/');
    }
});
