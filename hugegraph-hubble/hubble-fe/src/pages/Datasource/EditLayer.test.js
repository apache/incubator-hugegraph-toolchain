/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import {useCallback, useState} from 'react';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EditLayer from './EditLayer';

jest.mock('react-i18next', () => ({
    initReactI18next: {type: '3rdParty', init: jest.fn()},
    useTranslation: () => ({t: key => key}),
}));

jest.mock('../../api', () => ({
    manage: {
        addDatasource: jest.fn(),
        checkDatasourceConnection: jest.fn(),
    },
}));

beforeEach(() => {
    jest.clearAllMocks();
    const api = jest.requireMock('../../api');
    api.manage.addDatasource.mockResolvedValue({status: 200});
    window.matchMedia = jest.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        addListener: jest.fn(),
        removeListener: jest.fn(),
    }));
});

const ReopenableEditLayer = ({refresh = jest.fn()}) => {
    const [visible, setVisible] = useState(true);
    const open = useCallback(() => setVisible(true), []);
    const close = useCallback(() => setVisible(false), []);

    return (
        <>
            <button onClick={open}>reopen datasource</button>
            <button onClick={close}>close datasource externally</button>
            <EditLayer
                visible={visible}
                edit={false}
                onCancel={close}
                refresh={refresh}
            />
        </>
    );
};

const selectTemplate = async label => {
    const template = screen.getAllByRole('combobox')[0];
    await userEvent.click(template);
    await userEvent.click(await screen.findByText(label));
};

const deferred = () => {
    let resolve;
    let reject;
    const promise = new Promise((onResolve, onReject) => {
        resolve = onResolve;
        reject = onReject;
    });
    return {promise, resolve, reject};
};

const expectFreshForm = () => {
    expect(screen.getByPlaceholderText('datasource.form.name_placeholder')).toHaveValue('');
    expect(screen.queryByText('FILE', {selector: '.ant-select-selection-item'}))
        .not.toBeInTheDocument();
    expect(screen.queryByText('JDBC', {selector: '.ant-select-selection-item'}))
        .not.toBeInTheDocument();
    expect(screen.queryByText('datasource.form.templates.local_csv', {
        selector: '.ant-select-selection-item',
    })).not.toBeInTheDocument();
    expect(screen.queryByText('datasource.form.templates.jdbc_mysql', {
        selector: '.ant-select-selection-item',
    })).not.toBeInTheDocument();
    expect(screen.queryByText('datasource.form.config_info')).not.toBeInTheDocument();
};

test('clears the selected template label after a manual source-type change', async () => {
    render(
        <EditLayer
            visible
            edit={false}
            onCancel={jest.fn()}
            refresh={jest.fn()}
        />
    );

    const [template, type] = screen.getAllByRole('combobox');
    await userEvent.click(template);
    await userEvent.click(await screen.findByText('datasource.form.templates.local_csv'));
    expect(screen.getByText('datasource.form.templates.local_csv', {
        selector: '.ant-select-selection-item',
    })).toBeInTheDocument();

    fireEvent.mouseDown(type);
    await userEvent.click(await screen.findByText('JDBC'));

    await waitFor(() => expect(screen.queryByText(
        'datasource.form.templates.local_csv',
        {selector: '.ant-select-selection-item'}
    )).not.toBeInTheDocument());
    expect(screen.getByText('JDBC', {selector: '.ant-select-selection-item'}))
        .toBeInTheDocument();
});

test('clears template, type, and hidden config after cancel and reopen', async () => {
    render(<ReopenableEditLayer />);
    await selectTemplate('datasource.form.templates.local_csv');
    expect(screen.getByPlaceholderText('datasource.form.name_placeholder'))
        .toHaveValue('local_csv_example');

    await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    await userEvent.click(screen.getByRole('button', {name: 'reopen datasource'}));

    expectFreshForm();
});

test('clears template, type, and hidden config after successful create and reopen', async () => {
    const refresh = jest.fn();
    const api = jest.requireMock('../../api');
    render(<ReopenableEditLayer refresh={refresh} />);
    await selectTemplate('datasource.form.templates.jdbc_mysql');
    fireEvent.change(screen.getByPlaceholderText('datasource.form.url_placeholder'), {
        target: {value: 'jdbc:mysql://127.0.0.1:3306/example'},
    });
    fireEvent.change(screen.getByPlaceholderText('datasource.form.password_placeholder'), {
        target: {value: 'temporary-secret'},
    });

    await userEvent.click(screen.getByRole('button', {name: 'OK'}));
    await waitFor(() => expect(api.manage.addDatasource).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(refresh).toHaveBeenCalledTimes(1);

    await userEvent.click(screen.getByRole('button', {name: 'reopen datasource'}));
    expectFreshForm();
});

test('ignores a create response from a closed modal session', async () => {
    const api = jest.requireMock('../../api');
    const first = deferred();
    const second = deferred();
    const refresh = jest.fn();
    api.manage.addDatasource.mockReset();
    api.manage.addDatasource
        .mockReturnValueOnce(first.promise)
        .mockReturnValueOnce(second.promise);
    render(<ReopenableEditLayer refresh={refresh} />);

    await selectTemplate('datasource.form.templates.jdbc_mysql');
    fireEvent.change(screen.getByPlaceholderText('datasource.form.url_placeholder'), {
        target: {value: 'jdbc:mysql://127.0.0.1:3306/first'},
    });
    fireEvent.change(screen.getByPlaceholderText('datasource.form.password_placeholder'), {
        target: {value: 'first-secret'},
    });
    await userEvent.click(screen.getByRole('button', {name: 'OK'}));
    await waitFor(() => expect(api.manage.addDatasource).toHaveBeenCalledTimes(1));

    await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    await userEvent.click(screen.getByRole('button', {name: 'reopen datasource'}));
    await selectTemplate('datasource.form.templates.jdbc_mysql');
    fireEvent.change(screen.getByPlaceholderText('datasource.form.url_placeholder'), {
        target: {value: 'jdbc:mysql://127.0.0.1:3306/second'},
    });
    fireEvent.change(screen.getByPlaceholderText('datasource.form.password_placeholder'), {
        target: {value: 'second-secret'},
    });
    await userEvent.click(screen.getByRole('button', {name: 'OK'}));
    await waitFor(() => expect(api.manage.addDatasource).toHaveBeenCalledTimes(2));

    await act(async () => first.resolve({status: 200}));
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(refresh).not.toHaveBeenCalled();

    await act(async () => second.resolve({status: 200}));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(refresh).toHaveBeenCalledTimes(1);
});

test('ignores a create response after prop-driven modal closure', async () => {
    const api = jest.requireMock('../../api');
    const request = deferred();
    const refresh = jest.fn();
    api.manage.addDatasource.mockReset();
    api.manage.addDatasource.mockReturnValueOnce(request.promise);
    render(<ReopenableEditLayer refresh={refresh} />);

    await selectTemplate('datasource.form.templates.jdbc_mysql');
    fireEvent.change(screen.getByPlaceholderText('datasource.form.url_placeholder'), {
        target: {value: 'jdbc:mysql://127.0.0.1:3306/closed'},
    });
    fireEvent.change(screen.getByPlaceholderText('datasource.form.password_placeholder'), {
        target: {value: 'temporary-secret'},
    });
    await userEvent.click(screen.getByRole('button', {name: 'OK'}));
    await waitFor(() => expect(api.manage.addDatasource).toHaveBeenCalledTimes(1));

    await userEvent.click(screen.getByRole('button', {
        name: 'close datasource externally',
    }));
    await act(async () => request.resolve({status: 200}));

    expect(refresh).not.toHaveBeenCalled();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});
