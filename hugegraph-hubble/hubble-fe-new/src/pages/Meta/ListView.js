import {Tabs} from 'antd';
import PropertyTable from './Property';
import VertexTable from './Vertex';
import EdgeTable from './Edge';
import VertexIndexTable from './VertexIndex';
import EdgeIndexTable from './EdgeIndex';

const ListView = () => {
    return (
        <Tabs
            defaultActiveKey='1'
            destroyInactiveTabPane
            items={[
                {
                    label: '属性',
                    key: '1',
                    children: <PropertyTable />,
                },
                {
                    label: '顶点类型',
                    key: '2',
                    children: <VertexTable />,
                },
                {
                    label: '边类型',
                    key: '3',
                    children: <EdgeTable />,
                },
                {
                    label: '顶点索引',
                    key: '4',
                    children: <VertexIndexTable />,
                },
                {
                    label: '边索引',
                    key: '5',
                    children: <EdgeIndexTable />,
                },
            ]}
        />
    );
};

export default ListView;
