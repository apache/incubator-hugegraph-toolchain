/**
 * @file StepFormItem封装
 * @author gouzixing@
 */

import React, {useState, useCallback} from 'react';
import {Form, Input, Button, Tooltip} from 'antd';
import {RightOutlined, DownOutlined, PlusOutlined, QuestionCircleOutlined} from '@ant-design/icons';
import {propertiesValidator} from '../utils';
import classnames from 'classnames';
import c from './index.module.scss';

const description = {
    edge_steps: {
        label: '边类型',
        properties: '通过属性的值过滤边',
    },
    vertex_steps: {
        label: '点类型',
        properties: '通过属性的值过滤点',
    },
};

const StepsItems = props => {

    const {param, type, desc} = props;

    const [itemVisible, setItemVisible] = useState(true);

    const stepContentClassName = classnames(
        c.stepsItemsContent,
        {[c.contentHidden]: !itemVisible}
    );

    const changeItemVisibleState = useCallback(() => {
        setItemVisible(pre => !pre);
    }, []
    );

    const itemsContent = (name, type) => {
        return (
            <>
                <Form.Item
                    name={[name, 'label']}
                    label="label"
                    tooltip={description[type].label}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    name={[name, 'properties']}
                    label="properties"
                    tooltip={description[type].properties}
                    rules={[{validator: propertiesValidator}]}
                >
                    <Input.TextArea />
                </Form.Item>
            </>
        );
    };

    return (
        <Form.List
            name={[param, type]}
            initialValue={[{}]}
        >
            {(lists, {add, remove}, {errors}) => (
                <div className={c.stepsItemsContent}>
                    <div className={c.stepHeader} onClick={changeItemVisibleState}>
                        <div className={c.stepIcon}>
                            {itemVisible ? <DownOutlined /> : <RightOutlined />}
                        </div>
                        <div className={c.stepsItemsTitle}>{type}:</div>
                        <div className={c.tooltip}>
                            <Tooltip
                                placement="rightTop"
                                title={desc}
                            >
                                <QuestionCircleOutlined />
                            </Tooltip>
                        </div>
                    </div>
                    {

                        lists.map((item, name, index) => {
                            return (
                                <div key={item.key} className={stepContentClassName} style={{paddingLeft: '20px'}}>
                                    {itemsContent(name, type)}
                                    {lists.length > 1 ? (
                                        <Form.Item>
                                            <Button
                                                block
                                                danger
                                                onClick={() => remove(name)}
                                            >
                                                Delete
                                            </Button>
                                        </Form.Item>
                                    ) : null}
                                </div>
                            );
                        })
                    }
                    <Button
                        type="dashed"
                        onClick={() => {
                            add();
                            setItemVisible(true);
                        }}
                        style={{width: '100%'}}
                        icon={<PlusOutlined />}
                    >
                        Add
                    </Button>
                </div>
            )}
        </Form.List>
    );
};

export default StepsItems;