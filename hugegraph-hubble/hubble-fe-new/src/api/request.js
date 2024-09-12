import axios from 'axios';
import {message} from 'antd';
import JSONbig from 'json-bigint';
import _ from 'lodash';

const instance = axios.create({
    baseURL: '/api/v1.3',
    withCredentials: true,
    // 后端请求时延为30s，30s后后端统一处理返回400报错，这里设置31s为了尽可能不走timeout逻辑拿到超时报错。
    timeout: 31000,
    transformResponse: [data => {
        return JSONbig.parse(data);
    }],
});

instance.interceptors.request.use(
    config => {
        if (!config.headers['Content-Type']) {
            config.data = JSON.stringify(config.data);
            config.headers = {
                'Content-Type': 'application/json;charset=UTF-8',
            };
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

instance.interceptors.response.use(
    response => {
        if (response.data.status !== 200 && response.data.status !== 401) {
            if (!_.isEmpty(response.data.message)) {
                message.error(response.data.message);
            }
        }
        else if (response.data.status === 401) {
            // message.error('授权过期');
            localStorage.setItem('user', '');
            // storageFn.removeStorage(['lg','userInfo','tenant'])
            // setTimeout(() => {
            //     window.location = '/check';
            // }, 700);
        }
        return response;
    },
    error => {
        // if (!error.response) {
        //     setTimeout(() => {
        //         window.location = '/check';
        //     }, 700);

        //     return;
        // }
        const res = error.response?.data;
        message.error(`请求出错：${res.message ?? ''}，path：${res.path}`);
    }
);

const request = {};

request.get = async (url, params) => {
    const resposne = await instance.get(`${url}`, params);
    return resposne?.data;
};

request.post = async (url, params, config) => {
    const resposne = await instance.post(
        `${url}`,
        params,
        config
    );

    return resposne?.data;
};

request.put = async (url, params) => {
    const resposne = await instance.put(
        `${url}`,
        params
    );

    return resposne?.data;
};

request.delete = async (url, params) => {
    const resposne = await instance.delete(
        `${url}`,
        {params}
    );

    return resposne?.data;
};

export default request;
