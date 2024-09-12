import {useCallback} from 'react';
import {Button} from 'antd';

const GraphspaceButton = ({data, current, onClick, children}) => {
    const handleClick = useCallback(() => {
        onClick(data);
    }, [onClick, data]);

    return (
        <Button
            onClick={handleClick}
            type={current ? 'primary' : 'default'}
            shape="round"
            ghost={current}
        >
            {children}
        </Button>
    );
};

export default GraphspaceButton;
