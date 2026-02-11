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
 * @file  ToolBar
 * @author gouzixing
 */

import React, {useContext, useState, useEffect} from 'react';
import G6 from '@antv/g6';
import {GraphContext, ToolBarContext} from '../Context';
import c from './index.module.scss';
import classnames from 'classnames';

const ToolBar = props => {
    const {extra, hasPadding} = props;
    const context = useContext(GraphContext);
    const {graph} = context;

    const [toolBarContext, setToolBarContetx] = useState();

    const toolBarClassname = classnames(
        c.toolBarTop,
        {[c.toolBarWithPadding]: hasPadding}
    );

    useEffect(
        () => {
            const toolBarOptions = {
                getContent: () => {
                    return '<div />';
                },
            };
            const toolBarInstance = new G6.ToolBar(toolBarOptions);
            graph?.addPlugin(toolBarInstance);
            setToolBarContetx(toolBarInstance);
        },
        [graph]
    );

    return (
        <ToolBarContext.Provider value={toolBarContext}>
            <div className={toolBarClassname}>
                {extra && (
                    <>
                        {extra.map(
                            item => {
                                const {key, content} = item;
                                return (
                                    <a key={key}>
                                        {content}
                                    </a>
                                );
                            }
                        )}
                    </>
                )}
            </div>
        </ToolBarContext.Provider>
    );
};

export default ToolBar;
