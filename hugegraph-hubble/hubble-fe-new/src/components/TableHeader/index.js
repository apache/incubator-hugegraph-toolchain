import style from './index.module.scss';

const TableHeader = ({children}) => {
    return (
        <div className={style.header}>
            {children}
        </div>
    );
};

export default TableHeader;
