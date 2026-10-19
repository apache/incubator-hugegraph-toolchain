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

import {renderHook} from '@testing-library/react';
import useAlgorithmSubmission from './useAlgorithmSubmission';
import {GRAPH_STATUS} from '../../../utils/constants';

it('keeps each form completion through loading and terminal callbacks', () => {
    const first = jest.fn();
    const second = jest.fn();
    const begin = jest.fn().mockReturnValueOnce(first).mockReturnValueOnce(second);
    const formA = renderHook(() => useAlgorithmSubmission(begin));
    const formB = renderHook(() => useAlgorithmSubmission(begin));
    formA.result.current(GRAPH_STATUS.LOADING);
    formB.result.current(GRAPH_STATUS.LOADING);
    formB.result.current(GRAPH_STATUS.SUCCESS, {vertices: [{id: 'b'}]}, '', {layout: 'radial'});
    formA.result.current(GRAPH_STATUS.FAILED, {}, 'old failure');
    expect(begin).toHaveBeenCalledTimes(2);
    expect(first.mock.calls).toEqual([[GRAPH_STATUS.LOADING], [GRAPH_STATUS.FAILED, {}, 'old failure']]);
    expect(second.mock.calls).toEqual([
        [GRAPH_STATUS.LOADING],
        [GRAPH_STATUS.SUCCESS, {vertices: [{id: 'b'}]}, '', {layout: 'radial'}],
    ]);
    formB.result.current(GRAPH_STATUS.LOADING);
    expect(begin).toHaveBeenCalledTimes(3);
});
