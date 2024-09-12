import {useCallback} from 'react';
import style from './index.module.scss';

const StatusField = ({status}) => {
    const lower = status ? status.toLowerCase() : 'undefined';
    const config = {
        new: '新建',
        running: '执行中',
        success: '完成',
        cancelling: '停止',
        cancelled: '停止',
        failed: '失败',
        undefined: '未知',
    };

    return (
        <span className={`${style.state} ${config[lower] ? style[lower] : ''}`}>
            {config[lower] ? config[lower] : status}
        </span>
    );
};

const StatusA = ({onClick, disable, children}) => {

    return (
        <a
            onClick={disable ? null : onClick}
            className={disable ? style.disable : ''}
        >
            {children}
        </a>
    );
};

const StatusText = ({onClick, data, disable, children}) => {
    const handleClick = useCallback(() => {
        onClick(data);
    }, [onClick, data]);

    return (
        disable ? (
            <a className={style.disable}>{children}</a>
        ) : (
            <a onClick={handleClick}>{children}</a>
        )
    );
};

export {StatusField, StatusA, StatusText};
