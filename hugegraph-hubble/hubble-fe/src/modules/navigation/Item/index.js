/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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
        }
        else if (value.startsWith('/')) {
            navigate(value);
        }
        else {
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
                    <Button
                        block
                        type={'primary'}
                        onClick={() => {
                            onItemClick(url);
                        }}
                    >
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
