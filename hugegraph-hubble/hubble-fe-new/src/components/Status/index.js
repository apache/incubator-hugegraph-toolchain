import {useCallback} from 'react';
import style from './index.module.scss';
import {useTranslation} from 'react-i18next';

const StatusField = ({status}) => {
    const {t} = useTranslation();
    const lower = status ? status.toLowerCase() : 'undefined';
    const config = {
        new: t('common.status.new'),
        running: t('common.status.running'),
        success: t('common.status.success'),
        cancelling: t('common.status.cancelling'),
        cancelled: t('common.status.cancelled'),
        failed: t('common.status.failed'),
        undefined: t('common.status.undefined'),
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
