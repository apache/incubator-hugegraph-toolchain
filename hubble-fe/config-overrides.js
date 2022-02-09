const {
  override,
  addLessLoader,
  addWebpackAlias,
  overrideDevServer,
  watchAll
} = require('customize-cra');

const addProxy = () => (configFunction) => {
  configFunction.proxy = {
    '/about': {
      target: 'http://127:0.0.1:8088',
      changeOrigin: true
    },
    '/api': {
      target: 'http://127:0.0.1:8088',
      changeOrigin: true
    }
  };

  return configFunction;
};

module.exports = {
  webpack: override(
    addLessLoader({
      javascriptEnabled: true
    }),
    addWebpackAlias({
      'hubble-ui': require('path').resolve(__dirname, './src/components/hubble-ui')
    })
  ),
  devServer: overrideDevServer(addProxy(), watchAll())
};
