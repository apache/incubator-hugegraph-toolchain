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

import {act, fireEvent, render, screen} from '@testing-library/react';
import GraphAnalysisHome from './index';
import * as api from '../../api';

jest.mock('../../api', () => ({
    analysis: {
        getGraphSpaceList: jest.fn(),
        getOlapMode: jest.fn(),
        switchOlapMode: jest.fn(),
    },
    manage: {getGraphList: jest.fn(), getGraph: jest.fn()},
    auth: {getVermeer: jest.fn()},
}));
jest.mock('react-router-dom', () => ({
    useNavigate: () => jest.fn(),
    useParams: () => ({graphSpace: 'DEFAULT', graph: 'g'}),
}));
jest.mock('../analysis/Home', () => ({toolbarExtra}) => <div>{toolbarExtra}</div>);
jest.mock('../algorithm/Home', () => () => <div />);
jest.mock('../asyncTasks/Home', () => () => <div />);
jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));

it('retains a successful user switch when loading metadata polls before its response', async () => {
    jest.useFakeTimers();
    let resolveSwitch;
    const switching = new Promise(resolve => { resolveSwitch = resolve; });
    api.auth.getVermeer.mockResolvedValue({status: 200, data: {enable: true}});
    api.analysis.getGraphSpaceList.mockResolvedValue({status: 200, data: {graphspaces: ['DEFAULT']}});
    api.manage.getGraphList.mockResolvedValue({status: 200, data: {records: [{name: 'g'}]}});
    api.manage.getGraph.mockResolvedValue({status: 200, data: {name: 'g', status: 'loading'}});
    api.analysis.getOlapMode.mockResolvedValue({status: 200, data: {status: '1'}});
    api.analysis.switchOlapMode.mockReturnValue(switching);
    const view = render(<GraphAnalysisHome moduleName='gremlin' />);
    try {
        await act(async () => {});
        const toggle = screen.getByRole('switch');
        expect(toggle).not.toBeChecked();
        fireEvent.click(toggle);
        expect(api.analysis.switchOlapMode).toHaveBeenCalledTimes(1);
        // Native loading completes during the pending independent Server PUT.
        api.manage.getGraph.mockResolvedValue({status: 200, data: {name: 'g', status: 'loaded'}});
        await act(async () => jest.advanceTimersByTime(2000));
        expect(screen.getByText('analysis.topbar.loaded')).toBeVisible();
        await act(async () => resolveSwitch({status: 200}));
        const readsAfter = api.analysis.getOlapMode.mock.calls.length;
        await act(async () => jest.advanceTimersByTime(4000));
        expect(api.analysis.getOlapMode).toHaveBeenCalledTimes(readsAfter);
        expect(toggle).toBeChecked();
    }
    finally {
        view.unmount();
        jest.useRealTimers();
    }
});
