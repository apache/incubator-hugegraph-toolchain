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
