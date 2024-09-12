const sourceType = [
    {label: 'HDFS', value: 'HDFS'},
    {label: '本地上传', value: 'FILE'},
    {label: 'Kafka', value: 'KAFKA'},
    {label: 'JDBC', value: 'JDBC'},
];

const syncType = [
    {label: '执行一次', value: 'ONCE'},
    {label: '实时执行', value: 'REALTIME'},
    {label: '周期执行', value: 'CRON'},
];

export {sourceType, syncType};
