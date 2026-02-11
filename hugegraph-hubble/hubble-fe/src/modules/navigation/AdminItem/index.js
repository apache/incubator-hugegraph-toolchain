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
 * @file 系统管理子项块
 * @author
 */

import Item from '../Item';

const AdminItem = () => {
    return (
        <Item
            btnIndex={3}
            btnTitle={'系统管理'}
            listData={[
                {
                    title: '超管管理',
                    url: '/super',
                },
                {
                    title: '账号管理',
                    url: '/account',
                },
                {
                    title: '资源管理',
                    url: '/resource',
                },
                {
                    title: '角色管理',
                    url: '/role',
                },
            ]}
        />
    );
};

export default AdminItem;
