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
import {fireEvent, render, screen} from '@testing-library/react';
import Canvas3D from '.';
import RouteErrorBoundary from '../../../components/RouteErrorBoundary';

jest.mock('react-i18next', () => ({
    ...jest.requireActual('react-i18next'),
    useTranslation: () => ({t: key => key}),
}));
jest.mock('./Renderer', () => {
    throw new Error('3D chunk failed to load');
});

const Analysis = () => {
    const [is3D, setIs3D] = useState(true);
    return <>
        <div>Analysis menu</div>
        {is3D
            ? <Canvas3D data={{nodes: [], edges: []}} onExit={() => setIs3D(false)}>
                <div>Canvas settings remain available</div>
            </Canvas3D>
            : <div>Restored 2D canvas</div>}
    </>;
};

test('contains a rejected lazy import and returns to 2D without losing the analysis route', async () => {
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
    try {
        render(<RouteErrorBoundary fallback={<div>Analysis route lost</div>}><Analysis /></RouteErrorBoundary>);

        expect(await screen.findByRole('alert')).toHaveTextContent('analysis.canvas.canvas_3d.unavailable');
        expect(screen.getByRole('alert')).toHaveTextContent('analysis.canvas.canvas_3d.refresh_hint');
        expect(screen.getByText('Analysis menu')).toBeInTheDocument();
        expect(screen.getByText('Canvas settings remain available')).toBeInTheDocument();
        expect(screen.queryByText('Analysis route lost')).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'analysis.canvas.canvas_3d.back_to_2d'}));
        expect(screen.getByText('Restored 2D canvas')).toBeInTheDocument();
        expect(screen.getByText('Analysis menu')).toBeInTheDocument();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    } finally {
        consoleError.mockRestore();
    }
});
