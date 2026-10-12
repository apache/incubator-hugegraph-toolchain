/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {render, screen} from '@testing-library/react';
import PropertyTable from './Property';
import VertexTable from './Vertex';
import EdgeTable from './Edge';
import * as api from '../../api';

let mockRows = [];

jest.mock('react-i18next', () => ({
    useTranslation: () => ({t: key => key}),
}));

jest.mock('react-router-dom', () => ({
    useParams: () => ({graphspace: 'space', graph: 'graph'}),
}));

jest.mock('./common/useMetaTable', () => () => ({
    data: mockRows,
    pagination: {current: 1, pageSize: 10, total: mockRows.length},
    loading: false,
    error: false,
    retry: jest.fn(),
    handleTable: jest.fn(),
}));

jest.mock('./Property/EditLayer', () => ({EditPropertyLayer: () => (
    <div>property editor</div>
)}));
jest.mock('./Vertex/EditLayer', () => ({EditVertexLayer: () => (
    <div>vertex editor</div>
)}));
jest.mock('./Edge/EditLayer', () => ({EditEdgeLayer: () => (
    <div>edge editor</div>
)}));

jest.mock('../../api', () => ({
    manage: {
        getMetaPropertyList: jest.fn(),
        getMetaVertexList: jest.fn(),
    },
}));

beforeEach(() => {
    window.matchMedia = jest.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        addListener: jest.fn(),
        removeListener: jest.fn(),
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
    }));
});

beforeEach(() => {
    jest.clearAllMocks();
    mockRows = [];
    api.manage.getMetaPropertyList.mockReturnValue(new Promise(() => {}));
    api.manage.getMetaVertexList.mockReturnValue(new Promise(() => {}));
});

test('keeps the property table read-only', () => {
    mockRows = [{
        name: 'name',
        data_type: 'TEXT',
        cardinality: 'SINGLE',
    }];

    render(<PropertyTable canWrite={false} />);

    expect(screen.getByText('name')).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'common.action.refresh'}))
        .toBeInTheDocument();
    expect(screen.queryByRole('button', {name: 'common.action.create'}))
        .not.toBeInTheDocument();
    expect(screen.queryByText('common.action.delete')).not.toBeInTheDocument();
    expect(screen.queryByText('property editor')).not.toBeInTheDocument();
});

test('keeps the vertex table read-only', () => {
    mockRows = [{
        name: 'person',
        properties: [],
        id_strategy: 'PRIMARY_KEY',
        primary_keys: [],
        open_label_index: false,
        property_indexes: [],
    }];

    render(<VertexTable canWrite={false} />);

    expect(screen.getByText('person')).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'common.refresh'}))
        .toBeInTheDocument();
    expect(screen.queryByRole('button', {name: 'common.create'}))
        .not.toBeInTheDocument();
    expect(screen.queryByText('common.edit')).not.toBeInTheDocument();
    expect(screen.queryByText('common.delete')).not.toBeInTheDocument();
    expect(screen.queryByText('vertex editor')).not.toBeInTheDocument();
});

test('keeps the edge table read-only', () => {
    mockRows = [{
        name: 'knows',
        edgelabel_type: 'NORMAL',
        source_label: 'person',
        target_label: 'person',
        properties: [],
        sort_keys: [],
        open_label_index: false,
        property_indexes: [],
    }];

    render(<EdgeTable canWrite={false} />);

    expect(screen.getByText('knows')).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'common.refresh'}))
        .toBeInTheDocument();
    expect(screen.queryByRole('button', {name: 'common.create'}))
        .not.toBeInTheDocument();
    expect(screen.queryByText('common.edit')).not.toBeInTheDocument();
    expect(screen.queryByText('common.delete')).not.toBeInTheDocument();
    expect(screen.queryByText('edge editor')).not.toBeInTheDocument();
});
