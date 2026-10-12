/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
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

import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {message} from 'antd';
import EditLayer, {BUILTIN_SCHEMA_TEMPLATES} from './EditLayer';
import * as api from '../../api/index';

jest.mock('../../api/index', () => ({
    manage: {
        addSchema: jest.fn(),
        updateSchema: jest.fn(),
    },
}));

jest.mock('react-i18next', () => ({
    useTranslation: () => ({t: key => key}),
    initReactI18next: {
        type: '3rdParty',
        init: jest.fn(),
    },
}));

jest.mock('../../components/CodeEditor', () => props => (
    <textarea
        aria-label='schema-code-editor'
        value={props.value || ''}
        placeholder={props.placeholder}
        onChange={event => props.onChange?.(event.target.value)}
        data-min-height={props.minHeight}
    />
));

beforeAll(() => {
    window.matchMedia = window.matchMedia || (() => ({
        matches: false,
        addListener: jest.fn(),
        removeListener: jest.fn(),
    }));
});

beforeEach(() => {
    jest.clearAllMocks();
});

test('offers built-in starting points in create mode and fills an empty draft', async () => {
    render(
        <EditLayer
            visible
            mode='create'
            detail={{}}
            graphspace='DEFAULT'
            onCancel={jest.fn()}
            refresh={jest.fn()}
        />
    );

    const startingPoint = screen.getByRole('combobox');
    expect(startingPoint).toBeEnabled();
    await userEvent.click(startingPoint);
    await userEvent.click(screen.getByText('schema_template.builtin.people_network'));

    expect(screen.getByPlaceholderText('schema_template.form.name_placeholder'))
        .toHaveValue('people_network');
    expect(screen.getByPlaceholderText('schema_template.form.schema_placeholder'))
        .toHaveValue(BUILTIN_SCHEMA_TEMPLATES.people_network);
    expect(startingPoint).toBeDisabled();
});

test('opens a wide create dialog with a tall highlighted Schema editor', () => {
    render(
        <EditLayer
            visible
            mode='create'
            detail={{}}
            graphspace='DEFAULT'
            onCancel={jest.fn()}
            refresh={jest.fn()}
        />
    );

    expect(screen.getByRole('dialog')).toHaveStyle({width: '960px'});
    expect(screen.getByLabelText('schema-code-editor')).toHaveAttribute(
        'data-min-height',
        '360'
    );
});

test('uses a selected built-in starting point passed in from the Schema page', () => {
    render(
        <EditLayer
            visible
            mode='create'
            detail={{
                name: 'product_catalog',
                schema: BUILTIN_SCHEMA_TEMPLATES.product_catalog,
            }}
            graphspace='DEFAULT'
            onCancel={jest.fn()}
            refresh={jest.fn()}
        />
    );

    expect(screen.getByPlaceholderText('schema_template.form.name_placeholder'))
        .toHaveValue('product_catalog');
    expect(screen.getByLabelText('schema-code-editor'))
        .toHaveValue(BUILTIN_SCHEMA_TEMPLATES.product_catalog);
});

test('does not show the starting-point selector while editing', () => {
    render(
        <EditLayer
            visible
            mode='edit'
            detail={{name: 'existing', schema: 'schema = graph.schema()'}}
            graphspace='DEFAULT'
            onCancel={jest.fn()}
            refresh={jest.fn()}
        />
    );

    expect(screen.queryByText('schema_template.form.starting_point')).not.toBeInTheDocument();
});

test('keeps empty-field errors in the form without rejecting or calling the API', async () => {
    const unhandledRejection = jest.fn(event => event.preventDefault());
    window.addEventListener('unhandledrejection', unhandledRejection);
    render(
        <EditLayer
            visible
            mode='create'
            detail={{}}
            graphspace='DEFAULT'
            onCancel={jest.fn()}
            refresh={jest.fn()}
        />
    );

    await userEvent.click(screen.getByRole('button', {name: 'OK'}));

    await waitFor(() => expect(screen.getAllByRole('alert').length).toBeGreaterThan(0));
    expect(api.manage.addSchema).not.toHaveBeenCalled();
    expect(api.manage.updateSchema).not.toHaveBeenCalled();
    expect(unhandledRejection).not.toHaveBeenCalled();
    window.removeEventListener('unhandledrejection', unhandledRejection);
});

test('keeps invalid-name errors in the form without calling the API', async () => {
    render(
        <EditLayer
            visible
            mode='create'
            detail={{}}
            graphspace='DEFAULT'
            onCancel={jest.fn()}
            refresh={jest.fn()}
        />
    );

    await userEvent.type(
        screen.getByPlaceholderText('schema_template.form.name_placeholder'),
        'INVALID-NAME'
    );
    await userEvent.type(
        screen.getByPlaceholderText('schema_template.form.schema_placeholder'),
        'schema = graph.schema()'
    );
    await userEvent.click(screen.getByRole('button', {name: 'OK'}));

    await waitFor(() => expect(screen.getAllByRole('alert').length).toBeGreaterThan(0));
    expect(api.manage.addSchema).not.toHaveBeenCalled();
    expect(api.manage.updateSchema).not.toHaveBeenCalled();
});

test('keeps valid create submission behavior after validation passes', async () => {
    api.manage.addSchema.mockResolvedValue({status: 200});
    const onCancel = jest.fn();
    const refresh = jest.fn();
    render(
        <EditLayer
            visible
            mode='create'
            detail={{}}
            graphspace='DEFAULT'
            onCancel={onCancel}
            refresh={refresh}
        />
    );

    await userEvent.type(
        screen.getByPlaceholderText('schema_template.form.name_placeholder'),
        'valid_name'
    );
    await userEvent.type(
        screen.getByPlaceholderText('schema_template.form.schema_placeholder'),
        'schema = graph.schema()'
    );
    await userEvent.click(screen.getByRole('button', {name: 'OK'}));

    await waitFor(() => expect(api.manage.addSchema).toHaveBeenCalledWith(
        'DEFAULT',
        {
            name: 'valid_name',
            schema: 'schema = graph.schema()',
        },
        {suppressBusinessErrorToast: true}
    ));
    await waitFor(() => expect(onCancel).toHaveBeenCalled());
    expect(refresh).toHaveBeenCalled();
});

test('reports unexpected validation failures without an unhandled rejection', async () => {
    const error = new Error('validation infrastructure failed');
    const validateForm = jest.fn().mockRejectedValue(error);
    const unhandledRejection = jest.fn(event => event.preventDefault());
    const messageError = jest.spyOn(message, 'error').mockImplementation(() => undefined);
    window.addEventListener('unhandledrejection', unhandledRejection);
    render(
        <EditLayer
            visible
            mode='create'
            detail={{}}
            graphspace='DEFAULT'
            onCancel={jest.fn()}
            refresh={jest.fn()}
            validateForm={validateForm}
        />
    );

    await userEvent.click(screen.getByRole('button', {name: 'OK'}));

    await waitFor(() => expect(messageError).toHaveBeenCalledWith(
        'common.msg.operation_failed'
    ));
    expect(validateForm).toHaveBeenCalled();
    expect(api.manage.addSchema).not.toHaveBeenCalled();
    expect(api.manage.updateSchema).not.toHaveBeenCalled();
    expect(unhandledRejection).not.toHaveBeenCalled();
    window.removeEventListener('unhandledrejection', unhandledRejection);
});
