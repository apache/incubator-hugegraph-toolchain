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

import {render, screen} from '@testing-library/react';
import Canvas3D from '.';

const mockLoadRenderer = jest.fn();
const mockMountRenderer = jest.fn();
const mockCleanupRenderer = jest.fn();

jest.mock('./Renderer', () => {
    const {useEffect} = require('react');
    mockLoadRenderer();
    return {__esModule: true, default: function MockRenderer({data}) {
        useEffect(() => {
            mockMountRenderer(data);
            return mockCleanupRenderer;
        }, [data]);
        return <div>Loaded 3D renderer</div>;
    }};
});

test('loads the 3D implementation only on mount and unmounts it when leaving 3D', async () => {
    expect(mockLoadRenderer).not.toHaveBeenCalled();
    const data = {nodes: [{id: 'raw-node'}], edges: []};
    const {unmount} = render(<Canvas3D data={data} />);
    await screen.findByText('Loaded 3D renderer');

    expect(mockLoadRenderer).toHaveBeenCalledTimes(1);
    expect(mockMountRenderer).toHaveBeenCalledWith(data);
    expect(mockCleanupRenderer).not.toHaveBeenCalled();
    unmount();
    expect(mockCleanupRenderer).toHaveBeenCalledTimes(1);
});
