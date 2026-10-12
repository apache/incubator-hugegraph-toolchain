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

import {act, render, screen, waitFor} from '@testing-library/react';
import Louvain from './index';
import GraphAnalysisContext from '../../../../Context';

const mockSubmit = jest.fn();
const mockValidateFields = jest.fn(() => Promise.resolve());

jest.mock('../../PersistentForm', () => {
    const Form = ({children}) => <form>{children}</form>;
    Form.Item = ({children}) => <div>{children}</div>;
    Form.useForm = () => [{submit: mockSubmit, validateFields: mockValidateFields}];
    return Form;
});
jest.mock('../../AlgorithmNameHeader', () => props => (
    <button aria-label='run Louvain' disabled={props.isDisabled} />
));
jest.mock('../OlapComputerItem', () => () => null);
jest.mock('../../../../../api', () => ({
    analysis: {postOlapInfo: jest.fn()},
}));
jest.mock('react-i18next', () => ({
    useTranslation: () => ({t: key => key}),
}));

const renderLouvain = canRun => render(
    <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'hugegraph'}}>
        <Louvain
            canRun={canRun}
            handleFormSubmit={jest.fn()}
            updateCurrentAlgorithm={jest.fn()}
        />
    </GraphAnalysisContext.Provider>
);

beforeEach(() => {
    mockSubmit.mockClear();
    mockValidateFields.mockClear();
    mockValidateFields.mockResolvedValue();
});

test('does not offer Louvain when the graph has no edges', async () => {
    renderLouvain(false);

    await waitFor(() => expect(screen.getByRole('button', {
        name: 'run Louvain',
    })).toBeDisabled());
});

test('offers Louvain when edge data exists and its fixed form is valid', async () => {
    renderLouvain(true);

    await waitFor(() => expect(screen.getByRole('button', {
        name: 'run Louvain',
    })).toBeEnabled());
});

test('ignores a late valid result after switching to a graph without edges', async () => {
    let resolveValidation;
    mockValidateFields.mockReturnValueOnce(new Promise(resolve => {
        resolveValidation = resolve;
    }));
    const view = renderLouvain(true);

    view.rerender(
        <GraphAnalysisContext.Provider value={{graphSpace: 'DEFAULT', graph: 'empty'}}>
            <Louvain
                canRun={false}
                handleFormSubmit={jest.fn()}
                updateCurrentAlgorithm={jest.fn()}
            />
        </GraphAnalysisContext.Provider>
    );
    await act(async () => resolveValidation());

    expect(screen.getByRole('button', {
        name: 'run Louvain',
    })).toBeDisabled();
});
