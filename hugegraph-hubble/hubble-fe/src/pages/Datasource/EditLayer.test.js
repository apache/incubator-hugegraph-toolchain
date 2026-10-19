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
import {message} from 'antd';
import {act, fireEvent, render, screen, waitFor, within} from '@testing-library/react';
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
    Object.values(jest.requireMock('../../api').manage).forEach(mock => mock.mockReset());
    jest.spyOn(message, 'success').mockImplementation(() => undefined);
    jest.spyOn(message, 'error').mockImplementation(() => undefined);
    const api = jest.requireMock('../../api');
    api.manage.addDatasource.mockResolvedValue({status: 200});
    window.matchMedia = jest.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        addListener: jest.fn(),
        removeListener: jest.fn(),
    }));
});

const pendingResponses = new Set();

afterEach(async () => {
    try {
        await act(async () => {
            pendingResponses.forEach(resolve => resolve({status: 499}));
            pendingResponses.clear();
        });
    }
    finally {
        jest.restoreAllMocks();
    }
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

const clickCurrentOption = async (combobox, label) => {
    const list = document.getElementById(combobox.getAttribute('aria-controls'));
    expect(list).toBeInTheDocument();
    const popup = list.closest('.ant-select-dropdown');
    expect(popup).toBeInTheDocument();
    await userEvent.click(await within(popup).findByText(label, {
        selector: '.ant-select-item-option-content',
    }));
};

const selectTemplate = async (label, dialog = screen.getByRole('dialog')) => {
    // The template Form.Item has no name/htmlFor; use its initial order within this modal.
    const template = within(dialog).getAllByRole('combobox')[0];
    await userEvent.click(template);
    await clickCurrentOption(template, label);
    await waitFor(() => expect(within(dialog).getByText(label, {
        selector: '.ant-select-content',
    })).toBeInTheDocument());
    return dialog;
};

const deferred = () => {
    let resolve;
    let reject;
    const promise = new Promise((onResolve, onReject) => {
        resolve = value => {
            pendingResponses.delete(resolve);
            onResolve(value);
        };
        reject = error => {
            pendingResponses.delete(resolve);
            onReject(error);
        };
    });
    pendingResponses.add(resolve);
    return {promise, resolve, reject};
};

const expectFreshForm = dialog => {
    const form = within(dialog);
    expect(form.getByPlaceholderText('datasource.form.name_placeholder')).toHaveValue('');
    expect(form.queryByText('FILE', {selector: '.ant-select-content'}))
        .not.toBeInTheDocument();
    expect(form.queryByText('JDBC', {selector: '.ant-select-content'}))
        .not.toBeInTheDocument();
    expect(form.queryByText('datasource.form.templates.local_csv', {
        selector: '.ant-select-content',
    })).not.toBeInTheDocument();
    expect(form.queryByText('datasource.form.templates.jdbc_mysql', {
        selector: '.ant-select-content',
    })).not.toBeInTheDocument();
    expect(form.queryByText('datasource.form.config_info')).not.toBeInTheDocument();
};

// Real AntD Select interactions exceed 5s on CI; keep the inner waitFor deadlines.
test('clears the selected template label after a manual source-type change', async () => {
    render(
        <EditLayer
            visible
            edit={false}
            onCancel={jest.fn()}
            refresh={jest.fn()}
        />
    );

    const dialog = await screen.findByRole('dialog');
    const type = within(dialog).getByRole('combobox', {name: /^datasource\.form\.type/u});
    await selectTemplate('datasource.form.templates.local_csv', dialog);

    fireEvent.mouseDown(type);
    await clickCurrentOption(type, 'JDBC');
    await waitFor(() => {
        expect(within(dialog).queryByText('datasource.form.templates.local_csv', {
            selector: '.ant-select-content',
        })).not.toBeInTheDocument();
        expect(within(dialog).getByText('JDBC', {selector: '.ant-select-content'}))
            .toBeInTheDocument();
        expect(within(dialog).getByPlaceholderText('datasource.form.url_placeholder'))
            .toBeInTheDocument();
    });
}, 10000);

// This flow includes real template selection, modal closure, and reopening.
test('clears template, type, and hidden config after cancel and reopen', async () => {
    render(<ReopenableEditLayer />);
    const dialog = await screen.findByRole('dialog');
    await selectTemplate('datasource.form.templates.local_csv', dialog);
    expect(within(dialog).getByPlaceholderText('datasource.form.name_placeholder'))
        .toHaveValue('local_csv_example');

    const footer = dialog.querySelector('.ant-modal-footer');
    await userEvent.click(within(footer).getByRole('button', {name: 'Cancel'}));
    await waitFor(() => expect(dialog).not.toBeInTheDocument());
    await userEvent.click(screen.getByRole('button', {
        name: 'reopen datasource',
    }));
    const reopened = await screen.findByRole('dialog');
    expect(reopened).not.toBe(dialog);
    await waitFor(() => expectFreshForm(reopened));
}, 10000);

// Multi-step real AntD flows need a bounded CI budget; waitFor deadlines stay unchanged.
test('clears template, type, and hidden config after successful create and reopen', async () => {
    const refresh = jest.fn();
    const api = jest.requireMock('../../api');
    render(<ReopenableEditLayer refresh={refresh} />);
    const dialog = await selectTemplate('datasource.form.templates.jdbc_mysql');
    fireEvent.change(screen.getByPlaceholderText('datasource.form.url_placeholder'), {
        target: {value: 'jdbc:mysql://127.0.0.1:3306/example'},
    });
    fireEvent.change(screen.getByPlaceholderText('datasource.form.password_placeholder'), {
        target: {value: 'temporary-secret'},
    });

    await userEvent.click(screen.getByRole('button', {name: 'OK'}));
    await waitFor(() => expect(api.manage.addDatasource).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(dialog).not.toBeInTheDocument());
    expect(refresh).toHaveBeenCalledTimes(1);

    await userEvent.click(screen.getByRole('button', {name: 'reopen datasource'}));
    await waitFor(() => expectFreshForm(screen.getByRole('dialog')));
}, 10000);

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

    const firstDialog = await selectTemplate('datasource.form.templates.jdbc_mysql');
    fireEvent.change(screen.getByPlaceholderText('datasource.form.url_placeholder'), {
        target: {value: 'jdbc:mysql://127.0.0.1:3306/first'},
    });
    fireEvent.change(screen.getByPlaceholderText('datasource.form.password_placeholder'), {
        target: {value: 'first-secret'},
    });
    await userEvent.click(screen.getByRole('button', {name: 'OK'}));
    await waitFor(() => expect(api.manage.addDatasource).toHaveBeenCalledTimes(1));

    await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));
    await waitFor(() => expect(firstDialog).not.toBeInTheDocument());
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
    expect(message.success).not.toHaveBeenCalled();

    await act(async () => second.resolve({status: 200}));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(refresh).toHaveBeenCalledTimes(1);
    expect(message.success).toHaveBeenCalledTimes(1);
}, 20000); // Two complete real modal/form sessions, including async closure and reopening.

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
}, 10000);
