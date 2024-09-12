/**
 * @file 绿色标题块
 * @author
 */

import style from './index.module.scss';

const ModuleButton = props => {
    const {
        index,
        title,
    } = props;

    return (
        <div className={style.button}>
            {index && <span className={style.index}>{index}</span>}
            {title}
        </div>
    );
};

export default ModuleButton;
