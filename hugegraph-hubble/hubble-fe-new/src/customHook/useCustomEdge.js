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