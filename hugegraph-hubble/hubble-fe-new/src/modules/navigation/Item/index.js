/**
 * @file 子项块
 * @author
 */


import {Button} from 'antd';

import ModuleButton from '../ModuleButton';
import {useNavigate} from 'react-router-dom';

import style from './index.module.scss';

const Item = props => {
    const {
        btnTitle,
        btnIndex,
        listData,
    } = props;

    const navigate = useNavigate();

    const onItemClick = value => {
        if (!value) {
            return;
        } else if (value.startsWith('/')) {
            navigate(value);
        } else {
            window.open(value);
        }
    };

    const renderList = () => {
        const res = [];
        for (let item of listData) {
            const {
                title,
                url,
            } = item;
            const content = (
                <div className={style.item} key={title}>
                    <Button block type={'primary'} onClick={() => {onItemClick(url);}}>
                        {title}
                    </Button>
                </div>
            );
            res.push(content);
        }
        return res;
    };
    return (
        <div className={style.container}>
            <ModuleButton index={btnIndex} title={btnTitle} />
            <div className={style.itemList}>
                {renderList()}
            </div>
        </div>
    );
};

export default Item;
