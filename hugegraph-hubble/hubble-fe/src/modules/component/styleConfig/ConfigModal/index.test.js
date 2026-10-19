/*
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

import {render, screen, fireEvent, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ConfigModal from './index';

jest.mock('react-i18next', () => ({useTranslation: () => ({t: key => key})}));
jest.mock('../../../../components/IconSelect', () => ({__esModule: true,
    default: ({value, onChange, disabled}) => <select aria-label='icon choice' disabled={disabled}
        value={value || ''} onChange={event => onChange(event.target.value)}>
        <option value=''>none</option><option value='UserOutlined'>UserOutlined</option>
        <option value='HomeOutlined'>HomeOutlined</option>
    </select>}));
jest.mock('../../../../components/ColorSelect', () => ({InputColorSelect: ({value, onChange, disable}) =>
    <input aria-label='color' disabled={disable} value={value || ''} onChange={event => onChange(event.target.value)} />}));

beforeEach(() => {
    window.matchMedia = jest.fn(query => ({matches: false, media: query, onchange: null,
        addListener: jest.fn(), removeListener: jest.fn(), addEventListener: jest.fn(),
        removeEventListener: jest.fn(), dispatchEvent: jest.fn()}));
});

const fixture = () => ({nodes: {person: {type: 'circle', size: 30,
    style: {fill: '#123456', stroke: '#654321', lineWidth: 1, fillOpacity: 0.4},
    labelCfg: {style: {fontSize: 12, fill: '#333333'}},
    icon: {show: true, _iconName: 'UserOutlined', fontSize: 12}}},
    edges: {knows: {type: 'line', style: {stroke: '#123456', lineWidth: 1.6, endArrow: false},
        labelCfg: {style: {fontSize: 12, fill: '#333333'}}}}});
const choose = async (name, value) => {
    await userEvent.click(screen.getAllByRole('combobox', {name, exact: true})[0]);
    await userEvent.click(await screen.findByText(value, {selector: '.ant-select-item-option-content'}));
};

test('only opens a draft when visible, keeps edits across snapshots and discards them on close', async () => {
    const input = fixture();
    const onOk = jest.fn();
    const onCancel = jest.fn();
    const {rerender} = render(<ConfigModal visible={false} style={input} onOk={onOk} onCancel={onCancel} />);
    expect(screen.queryByRole('dialog')).toBeNull();
    rerender(<ConfigModal visible style={input} onOk={onOk} onCancel={onCancel} />);
    await choose('style_config.type', 'person');
    fireEvent.change(screen.getByRole('combobox', {name: 'icon choice'}), {target: {value: 'HomeOutlined'}});
    const newer = fixture();
    newer.nodes.person.style.fill = '#ff0000';
    rerender(<ConfigModal visible style={newer} onOk={onOk} onCancel={onCancel} />);
    await userEvent.click(screen.getByRole('button', {name: 'OK', exact: true}));
    expect(onOk).toHaveBeenCalledTimes(1);
    expect(onOk.mock.calls[0][0].nodes.person).toMatchObject({style: {fill: '#123456'},
        icon: {_iconName: 'HomeOutlined'}});
    expect(input.nodes.person.icon._iconName).toBe('UserOutlined');
    rerender(<ConfigModal visible={false} style={newer} onOk={onOk} onCancel={onCancel} />);
    rerender(<ConfigModal visible style={newer} onOk={onOk} onCancel={onCancel} />);
    await userEvent.click(screen.getByRole('button', {name: 'OK', exact: true}));
    expect(onOk.mock.calls[1][0].nodes.person.style.fill).toBe('#ff0000');
});

test('edge form loads the edge values and retains boolean arrows and zero width while changing color', async () => {
    const input = fixture();
    input.edges.knows.style.lineWidth = 0;
    const onOk = jest.fn();
    render(<ConfigModal visible style={input} onOk={onOk} onCancel={() => {}} />);
    await choose('style_config.appearance_type', 'style_config.edge_appearance');
    await choose('style_config.type', 'knows');
    expect(screen.getByRole('switch')).not.toBeChecked();
    fireEvent.change(screen.getAllByRole('textbox', {name: 'color'})[0], {target: {value: '#abcdef'}});
    await userEvent.click(screen.getByRole('switch'));
    await userEvent.click(screen.getByRole('button', {name: 'OK', exact: true}));
    await waitFor(() => expect(onOk).toHaveBeenCalledTimes(1));
    expect(onOk.mock.calls[0][0].edges.knows.style).toMatchObject({lineWidth: 0, endArrow: true, stroke: '#abcdef'});
    expect(onOk.mock.calls[0][0].edges.knows.style.endArrow).toBe(true);
    expect(input.edges.knows.style.endArrow).toBe(false);
});
