/**
 * @file 导航首页
 * @author
 */

import {PageHeader} from 'antd';
import ManageItem from '../ManageItem';
import AnalyseItem from '../AnalyseItem';
import AdminItem from '../AdminItem';
import ConsoleItem from '../ConsoleItem';
import * as user from '../../../utils/user';

import imgLogo from '../../../assets/logo_new.png';

import style from './index.module.scss';


const NavigationHome = () => {
    const userInfo = user.getUser();

    return (
        <>
            <PageHeader
                ghost={false}
                title="导航"
            />

            <div className={style.navigation}>
                <div className={style.header}>
                    <img width={'30%'} src={imgLogo} title={'logoNew'} alt={'logoNew'} />
                </div>
                <div className={style.container}>
                    <ManageItem />
                    <AnalyseItem />
                    {userInfo.is_superadmin && <AdminItem />}
                    {userInfo.is_superadmin && <ConsoleItem />}
                </div>
            </div>
        </>
    );
};

export default NavigationHome;
