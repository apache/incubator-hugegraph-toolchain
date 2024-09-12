const {createProxyMiddleware} = require('http-proxy-middleware');

module.exports = app => {
    app.use(
        '/api/v1.3',
        createProxyMiddleware({
            target: 'http://127.0.0.1:8088',
            changeOrigin: true,
            pathRewrite: {
                '^/api': '/api',
            },
        })
    );
};
