/**
 * @file  Panel操作按钮
 * @author
 */

import React, {useCallback} from 'react';
import {DoubleRightOutlined} from '@ant-design/icons';
import c from './index.module.scss';
import classnames from 'classnames';

const PanelControlButton = props => {
    const {
        show,
        onClick,
    } = props;

    const buttonClassnames = classnames(
        c.panelButton,
        {[c.panelButtonHidden]: !show}
    );

    const handleClickButton = useCallback(
        () => {
            onClick && onClick();
        },
        [onClick]
    );

    return (
        <div className={buttonClassnames} onClick={handleClickButton}>
            <DoubleRightOutlined />
        </div>
    );
};

export default PanelControlButton;
