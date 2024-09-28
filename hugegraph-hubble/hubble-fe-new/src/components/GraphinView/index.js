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

import {useCallback} from 'react';
import Graphin, {Behaviors} from '@antv/graphin';

const GraphView =  ({data, width, height, layout, style, onClick, config, behaviors}) => {
    // const [graphData, setGraphData] = useState([]);

    const {DragCanvas, ZoomCanvas, DragNode, ClickSelect, Hoverable} = Behaviors;
    const graphinLayout = {
        type: 'graphin-force',
        animation: false,
        ...layout,
        // type: 'preset',
    };

    const handleClickSelect = useCallback(evt => {
        const {item} = evt;
        const {id, type} = item._cfg;
        const model = item.getModel();

        typeof onClick === 'function' && onClick(id, type, model.data, model, item, evt);
    }, [onClick]);

    return (
        <div>
            <Graphin
                data={data}
                layout={graphinLayout}
                width={width}
                height={height}
                style={style}
                {...config}
            >
                <DragCanvas {...behaviors?.dragCanvas} />
                <ZoomCanvas {...behaviors?.zoomCanvas} />
                <DragNode {...behaviors?.dragNode} />
                <ClickSelect
                    selectEdge
                    onClick={handleClickSelect}
                    {...behaviors?.clickSelect}
                />
                <Hoverable bindType="edge" {...behaviors?.hoverable} />
                <Hoverable bindType="node" {...behaviors?.hoverable} />
                {/* <ActivateRelations trigger='click' /> */}
            </Graphin>
        </div>
    );
};

export default GraphView;
