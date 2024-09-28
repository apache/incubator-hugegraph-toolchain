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
 * @file 图分析模块Header，用于初始化、选择图空间和图，以及OLAP开关
 * @author gouzixing@
 */

import React, {useCallback, useEffect, useState, useContext} from 'react';
import {Select, Switch, Button, Tag, message, Typography, Tooltip} from 'antd';
import {SyncOutlined, QuestionCircleOutlined} from '@ant-design/icons';
import GraphAnalysisContext from '../../Context';
import * as api from '../../../api';
import {format} from 'date-fns';
import {GRAPH_LOAD_STATUS} from '../../../utils/constants';
import {useNavigate, useParams} from 'react-router-dom';
import _ from 'lodash';
import c from './index.module.scss';

const {Text} = Typography;

const {
    LOADED,
    LOADING,
    CREATED,
    ERROR,
} = GRAPH_LOAD_STATUS;

const TopBar = props => {
    const {
        onGraphInfoChange,
        showOlapSwitch,
        showNavigationButton,
        isOlapModeEnable,
        isOlapModeLoading,
        onOlapModeChange,
    } = props;

    const navigate = useNavigate();
    const {graphSpace: graphSpaceFromParam, graph: graphFromParam} = useParams();

    const {isVermeer} = useContext(GraphAnalysisContext);
    const [graphSpaceList, setGraphSpaceList] = useState([]);
    const [currentGraphSpace, setCurrentGraphSpace] = useState();
    const [isGraphSpaceLoading, setGraphSpaceLoading] = useState(false);
    const [graphList, setGraphList] = useState([]);
    const [currentGraph, setCurrentGraph] = useState({});
    const [isGraphLoading, setGraphLoading] = useState(false);
    const [isLoadRequestLoading, setLoadRequestLoading] = useState(false);

    const {
        status: currentGraphStatus,
        last_load_time: currentGraphLoadTime,
    } = currentGraph;

    const showVermeerGraphInfo = isVermeer && !_.isEmpty(currentGraph);

    const renderLoadTime = () => {
        return format(Date.parse(currentGraphLoadTime), 'yyyy-MM-dd');
    };

    const renderStatusTag = status => {
        if (isLoadRequestLoading) {
            return <Tag icon={<SyncOutlined spin />} color="processing" />;
        }
        switch (status) {
            case LOADED:
                return <Tag color="success">已加载</Tag>;
            case LOADING:
                return <Tag color="processing">加载中</Tag>;
            case ERROR:
                return <Tag color="error">加载失败</Tag>;
            case CREATED:
                return <Tag color="default">未加载</Tag>;
            default:
                return null;
        }
    };

    const getGraphSpaceOptions = () => {
        return graphSpaceList?.map(item => ({
            value: item,
            label: item,
        }));
    };

    const getGraphOptions = () => {
        return graphList?.map(item => {
            const {name, status} = item || {};
            const statusTag = renderStatusTag(status);
            return {
                value: name,
                label: (
                    <div className={c.graphOptions}>
                        <Text className={c.graphName} ellipsis={{tooltip: name}}>{name}</Text>
                        <span className={c.graphStatus}>{statusTag}</span>
                    </div>),
            };
        });
    };

    const getGraphSpaces = useCallback(
        async () => {
            setGraphSpaceLoading(true);
            const response = await api.analysis.getGraphSpaceList();
            const {status, data} = response || {};
            if (status === 200) {
                const {graphspaces} = data;
                setGraphSpaceList(graphspaces);
                if (!_.isEmpty(graphspaces)) {
                    // 如果有路由参数则为路由参数，否则为列表第一项；
                    setCurrentGraphSpace(graphSpaceFromParam || graphspaces[0]);
                }
            }
            setGraphSpaceLoading(false);
        },
        [graphSpaceFromParam]
    );

    const getGraphs = useCallback(
        async () => {
            setGraphLoading(true);
            const response = await api.analysis.getGraphList(currentGraphSpace);
            const {status, data} = response || {};
            if (status === 200) {
                const {graphs = []} = data;
                setGraphList(graphs);
                if (!_.isEmpty(graphs)) {
                    // 如果有路由参数且列表中可以找到(存在有路由参数但是切换空间)，则设置为路由参数，否则为列表第一项；
                    const graph = _.find(graphs, {name: graphFromParam}) || graphs[0];
                    setCurrentGraph(graph);
                }
            }
            setGraphLoading(false);
            setLoadRequestLoading(false);
        },
        [currentGraphSpace, graphFromParam]
    );

    useEffect(
        () => {
            if (!currentGraphSpace && _.isEmpty(currentGraph)) {
                getGraphSpaces();
            }
        },
        [currentGraph, currentGraphSpace, getGraphSpaces]
    );

    useEffect(
        () => {
            if (currentGraphSpace && _.isEmpty(currentGraph)) {
                getGraphs();
            }
        },
        [currentGraph, currentGraphSpace, getGraphs]
    );

    useEffect(
        () => {
            onGraphInfoChange && onGraphInfoChange(currentGraphSpace, currentGraph);
        },
        [currentGraph, currentGraphSpace, onGraphInfoChange]
    );

    const handleGraphSpaceChange = useCallback(
        value => {
            setCurrentGraphSpace(value);
            setCurrentGraph({});
        },
        []
    );

    const handleGraphChange = useCallback(
        value => {
            const currentGraphInfo = _.find(graphList, {name: value});
            setCurrentGraph(currentGraphInfo);
        },
        [graphList]
    );

    const handleSwitchOlapMode = useCallback(
        checked => {
            onOlapModeChange(checked);
        },
        [onOlapModeChange]
    );

    const onStatusBtnClick = () => {
        navigate('/asyncTasks');
    };

    const onLoadBtnClick = async () => {
        const params = {
            graphspace: currentGraphSpace,
            graph: currentGraph.name,
            task_type: currentGraphStatus === LOADED ? 'reload' : 'load',
        };

        setLoadRequestLoading(true);
        const res = await api.analysis.loadVermeerTask(params);
        const {status, message: errMsg} = res || {};
        if (status !== 200) {
            !errMsg && message.error('加载Vermeer任务失败');
            setLoadRequestLoading(false);
        }
        else {
            getGraphs();
        }
    };

    const handleClickNavigate = useCallback(
        () => {
            navigate(`/graphspace/${currentGraphSpace}/graph/${currentGraph?.name}/meta`);
        },
        [currentGraph.name, currentGraphSpace, navigate]
    );

    return (
        <div className={c.pageHeader}>
            <span>当前图空间:</span>
            <Select
                value={currentGraphSpace}
                onChange={handleGraphSpaceChange}
                options={getGraphSpaceOptions()}
                style={{width: 120}}
                bordered={false}
                loading={isGraphSpaceLoading}
            />
            <span>当前图:</span>
            <Select
                popupClassName={c.currentGraphSelect}
                value={currentGraph.name}
                onChange={handleGraphChange}
                options={getGraphOptions()}
                style={{width: 120}}
                bordered={false}
                loading={isGraphLoading}
                placeholder="请选择"
                optionLabelProp="value"
            />
            {
                showVermeerGraphInfo && (
                    <span className={c.vermeerInfo}>
                        <Button type="text" disabled={currentGraphStatus === CREATED} onClick={onStatusBtnClick}>
                            <span className={c.graphStatus}>{renderStatusTag(currentGraphStatus)}</span>
                        </Button>
                        {
                            currentGraphLoadTime && (
                                <span className={c.graphLoadTime}>
                                    <span>最近加载时间:</span><span>{renderLoadTime()}</span>
                                </span>)
                        }
                        <Button size='small' onClick={onLoadBtnClick} disabled={currentGraphStatus === LOADING}>
                            {currentGraphStatus === LOADED ? '重加载' : '加载'}到Vermeer
                        </Button>
                    </span>
                )
            }
            {
                showNavigationButton && (
                    <>
                        <Button
                            size='small'
                            onClick={handleClickNavigate}
                            disabled={_.isEmpty(currentGraph)}
                        >
                            <span>元数据配置</span>
                        </Button>
                        <Tooltip placement="bottom" title={'点击可跳转到对应图的元数据配置的页面'} className={c.questionCircleIcon}>
                            <QuestionCircleOutlined />
                        </Tooltip>
                    </>
                )
            }
            {
                showOlapSwitch && (
                    <div className={c.olapSwitchButton}>
                        <span className={c.olapSwitchTitle}>是否查询OLAP结果:</span>
                        <Switch
                            checked={isOlapModeEnable}
                            checkedChildren="开启"
                            unCheckedChildren="关闭"
                            onChange={handleSwitchOlapMode}
                            loading={isOlapModeLoading}
                        />
                    </div>
                )
            }
        </div>

    );
};

export default TopBar;
