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

/**
 * @file  自定义边
 * @author
 */

import G6 from '@antv/g6';
import {useEffect} from 'react';

const options = {
    afterDraw(cfg, group) {
        const shape = group.get('children')[0];
        const startPoint = shape.getPoint(0);
        const circle = group.addShape('circle', {
            attrs: {
                x: startPoint.x,
                y: startPoint.y,
                fill: cfg.style.stroke,
                r: 3,
            },
            name: 'circle-shape',
        });
        circle.animate(
            ratio => {
                const tmpPoint = shape.getPoint(ratio);
                return {
                    x: tmpPoint.x,
                    y: tmpPoint.y,
                };
            },
            {
                repeat: true, // Whether executes the animation repeatly
                duration: 3000, // the duration for executing once
            }
        );
    },
    update: undefined,
};

const useCustomEdge = () => {
    useEffect(
        () => {
            G6.registerEdge('runningLine', options, 'line');
            G6.registerEdge('runningQuadratic', options, 'quadratic');
            G6.registerEdge('runningLoop', options, 'loop');
        },
        []
    );
};

export default useCustomEdge;
