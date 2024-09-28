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

import {Modal, Button, Form, Input, Typography, Select,
    Divider, Upload, Radio, AutoComplete, Checkbox, message, Space} from 'antd';
import {useState, useCallback} from 'react';
import * as api from '../../api';
import * as rules from '../../utils/rules';

const compressionOptions = [
    'NONE',
    'GZIP',
    'BZ2',
    'XZ',
    'LZMA',
    'SNAPPY_RAW',
    'SNAPPY_FRAMED',
    'Z',
    'DEFLATE',
    'LZ4_BLOCK',
    'LZ4_FRAMED',
    'ORC',
    'PARQUET',
];

const fileAccept = {
    CSV: '.csv',
    TEXT: '.txt',
    JSON: '.json',
    GZIP: '.gz,.gzip',
    BZ2: '.bz2',
    XZ: '.xz',
    LZMA: '.lzma',
    SNAPPY_RAW: '.snappy',
    SNAPPY_FRAMED: '.snappy',
    Z: '.z',
    DEFLATE: '.deflate',
    ORC: '.orc',
    PARQUET: '.parquet',
    LZ4_FRAMED: '.lz4',
    LZ4_BLOCK: '.lz4',
};

const fileTypeOptions = [
    {label: '*.CSV', value: 'CSV'},
    {label: '*.TEXT', value: 'TEXT'},
    {label: '*.JSON', value: 'JSON'},
];

const charsetOptions = [
    {label: 'UTF-8', value: 'UTF-8'},
    {label: 'GBK', value: 'GBK'},
    {label: 'ISO-8859-1', value: 'ISO-8859-1'},
    {label: 'US-ASCII', value: 'US-ASCII'},
];

const dateformatOptions = [
    {label: 'yyyy-MM-dd', value: 'yyyy-MM-dd'},
    {label: 'yyyy-MM-dd HH:MM:SS', value: 'yyyy-MM-dd HH:MM:SS'},
    {label: 'yyyy-MM-dd HH:mm:ss.SSS', value: 'yyyy-MM-dd HH:mm:ss.SSS'},
];

const driverList = {
    'MySQL': 'com.mysql.cj.jdbc.Driver',
    'PostgreSQL': 'org.postgresql.Driver',
    'Oracle': 'oracle.jdbc.driver.OracleDriver',
    'SQLServer': 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
    'HIVE': 'org.apache.hive.jdbc.HiveDriver',
};

const normFile = e => {
    if (Array.isArray(e)) {
        return e;
    }

    const {fileList} = e;
    // 以下顺序不可变
    if (fileList.length === 0) {
        return null;
    }

    if (fileList[0].status === 'error') {
        return 'error';
    }

    if (fileList[0].status !== 'done') {
        return null;
    }

    if (fileList[0].response.status !== 200) {
        return 'error';
    }

    return fileList[0]?.response?.data?.file;
};

const formatDatasource = values => {
    const datasource_config = {...values};
    const datasource_name = datasource_config.datasource_name;
    datasource_config.header = datasource_config.header ? datasource_config.header.split(',') : null;
    delete datasource_config.datasource_name;

    return {datasource_name, datasource_config};
};

const UploadForm = ({label, name, accept}) => {
    const [visible, setVisible] = useState(true);

    const handleChange = useCallback(e => {
        setVisible(e.fileList.length === 0);
    }, []);

    return (
        <Form.Item
            label={label}
            rules={[
                rules.required(`请上传${label}`),
                () => ({
                    validator(_, value) {
                        if (value === 'error') {
                            return Promise.reject('上传失败');
                        }

                        return Promise.resolve();
                    },
                }),
            ]}
            name={name}
            valuePropName='file'
            getValueFromEvent={normFile}
        >
            <Upload
                action={api.manage.datasourceUploadUrl}
                maxCount={1}
                onChange={handleChange}
                accept={accept}
            >
                <Button type='link' disabled={!visible}>上传文件</Button>
            </Upload>
        </Form.Item>
    );
};

// 认证 form
const CertForm = ({isHive}) => {
    const [authType, setAuthType] = useState(0);

    const authTypeList = [
        {label: '无', value: 0},
        {label: 'kerberos认证', value: 1},
    ];

    const handleAuthType = useCallback(e => setAuthType(e.target.value), []);

    return (
        <>
            <Divider />
            <Typography.Title level={5}>认证信息</Typography.Title>
            <Form.Item label='特殊认证方式' rules={[rules.required()]}>
                <Radio.Group
                    onChange={handleAuthType}
                    options={authTypeList}
                    value={authType}
                />
            </Form.Item>
            {authType === 1 && !isHive
            && (
                <>
                    <UploadForm label='kertab文件' name={['kerberos_config', 'keytab']} />
                    <UploadForm label='conf文件' name={['kerberos_config', 'krb5_conf']} />
                    <Form.Item
                        label='principal'
                        rules={[rules.required()]}
                        name={['kerberos_config', 'principal']}
                    >
                        <Input showCount maxLength={50} />
                    </Form.Item>
                    <Form.Item name={['kerberos_config', 'enable']} hidden />
                </>
            )}
            {authType === 1 && isHive
            && (
                <>
                    <UploadForm label='krb5.conf' name={['principals', 'krb5.conf']} />
                    <Form.Item label='principal' rules={[rules.required()]} name={['principals', 'principal']}>
                        <Input />
                    </Form.Item>
                    <Form.Item label='user.name' rules={[rules.required()]} name={['principals', 'user.name']}>
                        <Input />
                    </Form.Item>
                    <UploadForm label='user.keytab' name={['principals', 'user.keytab']} />
                    <Form.Item label='zk.quorum' rules={[rules.required()]} name={['principals', 'zk.quorum']}>
                        <Input />
                    </Form.Item>
                </>
            )}
        </>
    );
};

// 本地上传form
const LocalFileForm = () => {
    const [fileType, setFileType] = useState('CSV');
    const [accept, setAccept] = useState('.csv');
    const [compression, setCompression] = useState('NONE');

    const handleCompression = useCallback(val => {
        setCompression(val);

        if (val === 'NONE') {
            setAccept(fileAccept[fileType]);
        }
        else {
            setAccept(fileAccept[val]);
        }
    }, [fileType]);

    const handleFormat = useCallback(val => {
        setFileType(val);

        if (compression === 'NONE') {
            setAccept(fileAccept[val]);
        }
    }, [compression]);

    return (
        <>
            <Divider />
            <Typography.Title level={5}>配置信息</Typography.Title>
            <Form.Item label='文件类型' wrapperCol={{span: 4}} rules={[rules.required()]} name='format'>
                <Select
                    options={fileTypeOptions}
                    onChange={handleFormat}
                    placeholder='请选择'
                />
            </Form.Item>
            <Form.Item label='header' name='header'>
                <Input placeholder='请输入header,以,分割' />
            </Form.Item>
            {(fileType === 'TEXT')
            && (
                <Form.Item label='列分隔符' wrapperCol={{span: 2}} name='delimiter'>
                    <Input />
                </Form.Item>
            )}
            <Form.Item label='编码字符集' wrapperCol={{span: 6}} rules={[rules.required()]} name='charset'>
                <AutoComplete options={charsetOptions} />
            </Form.Item>
            <Form.Item label='日期格式' wrapperCol={{span: 10}} rules={[rules.required()]} name='date_format'>
                {/* <Input placeholder='yyyy-MM-dd HH:mm:ss' /> */}
                <AutoComplete options={dateformatOptions} />
            </Form.Item>
            <Form.Item label='时区' wrapperCol={{span: 4}} rules={[rules.required()]} name='time_zone'>
                <Select
                    options={[...new Array(25).keys()].map(item => {
                        const str = item === 11 ? '' : (item > 11 ? `+${item - 12}` : item - 12);
                        return {label: `GMT${str}`, value: `GMT${str}`};
                    })}
                />
            </Form.Item>
            <Form.Item label='跳过行' wrapperCol={{span: 8}} rules={[rules.required()]} name={['skipped_line', 'regex']}>
                <Input placeholder='(^#|^//).*' />
            </Form.Item>
            <Form.Item label='压缩格式' wrapperCol={{span: 8}} rules={[rules.required()]} name='compression'>
                <Select
                    options={compressionOptions.map(item => ({label: item, value: item}))}
                    onChange={handleCompression}
                />
            </Form.Item>
            <UploadForm label='本地文件' name={'path'} accept={accept} />
            {/* <Form.Item
                label='本地文件'
                rules={[rules.required('请上传文件')]}
                name='path'
                valuePropName='file'
                getValueFromEvent={normFile}
            >
                <Upload
                    action={api.manage.datasourceUploadUrl}
                    maxCount={1}
                ><a>上传文件</a>
                </Upload>
            </Form.Item> */}
        </>
    );
};

// hdfs form
const HdfsForm = () => {
    const [fileType, setFileType] = useState('');

    const handleFileType = useCallback(val => setFileType(val), []);

    return (
        <>
            <Divider />
            <Typography.Title level={5}>配置信息</Typography.Title>
            <Form.Item label='path' rules={[rules.required()]} name='path'>
                <Input />
            </Form.Item>
            <UploadForm label='core_site' name={'core_site_path'} accept='.xml' />
            <UploadForm label='hdfs_site' name={'hdfs_site_path'} accept='.xml' />
            <Form.Item label='文件类型' wrapperCol={{span: 4}} rules={[rules.required()]} name='format'>
                <Select
                    options={fileTypeOptions}
                    onChange={handleFileType}
                    placeholder='请选择'
                />
            </Form.Item>
            <Form.Item label='header' name='header'>
                <Input placeholder='请输入header,以,分割' />
            </Form.Item>
            {(fileType === 'TEXT')
            && (
                <Form.Item label='列分隔符' wrapperCol={{span: 2}} name='delimiter'>
                    <Input />
                </Form.Item>
            )}
            <Form.Item label='编码字符集' wrapperCol={{span: 6}} rules={[rules.required()]} name='charset'>
                <AutoComplete options={charsetOptions} />
            </Form.Item>
            <Form.Item label='日期格式' wrapperCol={{span: 10}} rules={[rules.required()]} name='date_format'>
                <AutoComplete options={dateformatOptions} />
            </Form.Item>
            <Form.Item label='时区' wrapperCol={{span: 4}} rules={[rules.required()]} name='time_zone'>
                <Select
                    options={[...new Array(25).keys()].map(item => {
                        const str = item === 11 ? '' : (item > 11 ? `+${item - 12}` : item - 12);
                        return {label: `GMT${str}`, value: `GMT${str}`};
                    })}
                />
            </Form.Item>
            <Form.Item label='跳过行' wrapperCol={{span: 8}} rules={[rules.required()]} name={['skipped_line', 'regex']}>
                <Input placeholder='(^#|^//).*' />
            </Form.Item>
            <Form.Item label='压缩格式' wrapperCol={{span: 8}} rules={[rules.required()]} name='compression'>
                <Select
                    options={compressionOptions.map(item => ({label: item, value: item}))}
                />
            </Form.Item>
            <CertForm />
        </>
    );
};

// kafka form
const KafkaForm = () => {
    const [fileType, setFileType] = useState('');

    const handleFileType = useCallback(val => setFileType(val), []);

    return (
        <>
            <Divider />
            <Typography.Title level={5}>配置信息</Typography.Title>
            <Form.Item label='server' rules={[rules.required()]} name='bootstrap-server'>
                <Input placeholder='请输入kafka server list' />
            </Form.Item>
            <Form.Item label='topic' rules={[rules.required()]} name='topic'>
                <Input placeholder='请输入topic' />
            </Form.Item>
            <Form.Item label='是否从头读取' name='from-beginning' valuePropName='checked'>
                <Checkbox />
            </Form.Item>
            <Form.Item label='文件类型' wrapperCol={{span: 4}} rules={[rules.required()]} name='format'>
                <Select
                    options={fileTypeOptions}
                    onChange={handleFileType}
                    placeholder='请选择'
                />
            </Form.Item>
            <Form.Item label='header' name='header'>
                <Input placeholder='请输入header,以,分割' />
            </Form.Item>
            {(fileType === 'TEXT')
            && (
                <Form.Item label='列分隔符' wrapperCol={{span: 2}} name='delimiter'>
                    <Input />
                </Form.Item>
            )}
            <Form.Item label='编码字符集' wrapperCol={{span: 6}} rules={[rules.required()]} name='charset'>
                <AutoComplete options={charsetOptions} />
            </Form.Item>
            <Form.Item label='日期格式' wrapperCol={{span: 10}} rules={[rules.required()]} name='date_format'>
                {/* <Input placeholder='yyyy-MM-dd HH:mm:ss' /> */}
                <AutoComplete options={dateformatOptions} />
            </Form.Item>
            <Form.Item label='时区' wrapperCol={{span: 4}} rules={[rules.required()]} name='time_zone'>
                <Select
                    options={[...new Array(25).keys()].map(item => {
                        const str = item === 11 ? '' : (item > 11 ? `+${item - 12}` : item - 12);
                        return {label: `GMT${str}`, value: `GMT${str}`};
                    })}
                />
            </Form.Item>
            <Form.Item label='跳过行' wrapperCol={{span: 8}} rules={[rules.required()]} name={['skipped_line', 'regex']}>
                <Input placeholder='(^#|^//).*' />
            </Form.Item>
        </>
    );
};

const JDBCForm = ({setField, form}) => {
    const [vendor, setVendor] = useState('');
    const [status, setStatus] = useState('');

    const vendorOptions = [
        {label: 'MySQL', value: 'MySQL'},
        {label: 'PostgreSQL', value: 'PostgreSQL'},
        {label: 'Oracle', value: 'Oracle'},
        {label: 'SQLServer', value: 'SQLServer'},
        {label: 'HIVE', value: 'HIVE'},
    ];

    const handleTest = useCallback(() => {
        form.validateFields([
            'type', 'vendor', 'driver', 'url', 'database',
            'schema', 'table', 'username', 'password', 'batch_size', 'where',
        ]).then(values => {
            const formatData = formatDatasource(values);
            api.manage.checkJDBC(formatData).then(res => {
                if (res.status === 200) {
                    setStatus(res.data.result);
                    return;
                }

                message.error(res.message);
            });
        });
    }, [form]);

    const handleVendor = useCallback(val => {
        setField({'driver': driverList[val]});
        setVendor(val);
    }, [setField]);

    return (
        <>
            <Divider />
            <Typography.Title level={5}>配置信息</Typography.Title>
            <Form.Item label='数据库类型' rules={[rules.required()]} name='vendor'>
                <Select
                    options={vendorOptions}
                    onChange={handleVendor}
                    placeholder='请选择数据库类型'
                />
            </Form.Item>
            <Form.Item label='driver' name='driver'>
                <Input readOnly />
            </Form.Item>
            <Form.Item label='URL' rules={[rules.required(), rules.isJDBC]} name='url'>
                <Input placeholder='请输入URL' />
            </Form.Item>
            <Form.Item label='数据库名' rules={[rules.required()]} name='database'>
                <Input placeholder='请输入数据库名' />
            </Form.Item>
            {(vendor === 'Oracle' || vendor === 'PostgreSQL') && (
                <Form.Item label='schema' name='schema'>
                    <Input placeholder='请输入schema' />
                </Form.Item>)
            }
            {vendor === 'SQLServer' && (
                <Form.Item label='schema' name='schema' rules={[rules.required()]}>
                    <Input placeholder='请输入schema' />
                </Form.Item>)
            }
            <Form.Item label='表名' rules={[rules.required()]} name='table'>
                <Input placeholder='请输入表名' />
            </Form.Item>
            <Form.Item label='用户名' rules={[rules.required()]} name='username'>
                <Input placeholder='请输入用户名' />
            </Form.Item>
            <Form.Item label='密码' rules={[rules.required()]} name='password'>
                <Input.Password placeholder='请输入密码' autoComplete='new-password' />
            </Form.Item>
            <Form.Item label='页大小' name='batch_size' wrapperCol={{span: 4}} hidden>
                <Input />
            </Form.Item>
            {vendor === 'HIVE'
            && (
                <>
                    <Form.Item label='where语句' name='where'>
                        <Input placeholder='请输入where语句' />
                    </Form.Item>
                    <CertForm isHive />
                </>
            )}
            <Form.Item wrapperCol={{offset: 5}}>
                <Space>
                    <Button onClick={handleTest}>连接测试</Button>
                    <Typography.Text type={status === 'success' ? status : 'danger'}>{status}</Typography.Text>
                </Space>
            </Form.Item>
        </>
    );
};

const EditLayer = ({edit, visible, onCancel, refresh}) => {
    const [sourceType, setSourceType] = useState('');
    const [loading, setLoading] = useState(false);
    const [form] = Form.useForm();

    const sourceTypeOptions = [
        {label: 'HDFS', value: 'HDFS'},
        {label: '本地上传', value: 'FILE'},
        {label: 'Kafka', value: 'KAFKA'},
        {label: 'JDBC', value: 'JDBC'},
    ];

    const onFinish = useCallback(() => {
        if (loading) {
            return;
        }

        form.validateFields().then(values => {
            const formatData = formatDatasource(values);
            // return;
            setLoading(true);
            api.manage.addDatasource(formatData).then(res => {
                setLoading(false);

                if (res.status === 200) {
                    message.success('添加成功');
                    onCancel();
                    refresh();
                    return;
                }

                message.error(res.message);
            });
        });
    }, [form, loading, onCancel, refresh]);

    const handleClose = useCallback(() => setSourceType(''), []);

    const handleType = useCallback(val => setSourceType(val), []);

    return (
        <Modal
            title={edit ? '编辑数据源' : '新增数据源'}
            onCancel={onCancel}
            open={visible}
            width={600}
            onOk={onFinish}
            confirmLoading={loading}
            afterClose={handleClose}
            destroyOnClose
        >
            <Form
                form={form}
                labelCol={{span: 5}}
                preserve={false}
                initialValues={{
                    batch_size: 500,
                    split: ',',
                    date_format: 'yyyy-MM-dd HH:mm:ss',
                    skipped_line: {
                        regex: '(^#|^//).*',
                    },
                    charset: 'UTF-8',
                    time_zone: 'GMT+8',
                    compression: 'NONE',
                    kerberos_config: {
                        enable: true,
                    },
                    delimiter: ',',
                    'from-beginning': false,
                    format: 'CSV',
                }}
            >
                <Typography.Title level={5}>基础属性</Typography.Title>
                <Form.Item
                    label='数据源名称'
                    name='datasource_name'
                    rules={[rules.required(), {type: 'string', max: 50}, rules.isPropertyName()]}
                >
                    <Input showCount maxLength={50} placeholder='请输入数据源名称' />
                </Form.Item>
                <Form.Item label='数据源类型' name='type' rules={[rules.required()]}>
                    <Select
                        options={sourceTypeOptions}
                        onChange={handleType}
                        placeholder='请选择数据源类型'
                    />
                </Form.Item>

                {(sourceType === 'FILE')
                && (
                    <LocalFileForm />
                )}

                {sourceType === 'KAFKA'
                && (
                    <KafkaForm />
                )}

                {sourceType === 'HDFS'
                && (
                    <HdfsForm />
                )}

                {sourceType === 'JDBC'
                && (
                    <JDBCForm setField={form.setFieldsValue} form={form} />
                )}
            </Form>
        </Modal>
    );
};

export default EditLayer;
