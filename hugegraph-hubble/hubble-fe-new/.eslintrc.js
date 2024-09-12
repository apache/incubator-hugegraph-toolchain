module.exports = {
    'env': {
        'browser': true,
        'es2021': true,
    },
    'extends': [
        // "eslint:recommended",
        // "plugin:react/recommended",
        // "plugin:@typescript-eslint/recommended",

        '@ecomfe/eslint-config/baidu/default', // 根据代码库ES版本选择default或es5
        '@ecomfe/eslint-config/baidu/defect', // 根据代码库ES版本选择defect或defect-es5
        '@ecomfe/eslint-config',
        '@ecomfe/eslint-config/typescript',
        '@ecomfe/eslint-config/react',
    ],
    'parser': '@typescript-eslint/parser',
    'parserOptions': {
        'ecmaFeatures': {
            'jsx': true,
        },
        'ecmaVersion': 12,
        'sourceType': 'module',
    },
    'plugins': [
        'react',
        '@typescript-eslint',
    ],
    'rules': {
    },
};
