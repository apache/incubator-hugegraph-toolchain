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