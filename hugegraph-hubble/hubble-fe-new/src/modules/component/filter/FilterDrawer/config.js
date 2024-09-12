// powershell语法
const ruleOptions = [
    {value: 'gt', label: '>'},
    {value: 'gte', label: '>='},
    {value: 'lt', label: '<'},
    {value: 'lte', label: '<='},
    {value: 'eq', label: '='},
    {value: 'neq', label: '!='},
    {value: 'ltlt', label: '< value <'},
    {value: 'ltelt', label: '<= value <'},
    {value: 'ltlte', label: '< value <='},
    {value: 'ltelte', label: '<= value <='},
    {value: 'contains', label: 'contains'},
    {value: 'notcontains', label: 'not contains'},
];

const propertyDateOption = [
    {value: 'eq', label: '所选当天'},
    {value: 'lt', label: '之前（不包含所选日期）'},
    {value: 'gt', label: '之后（不包含所选日期）'},
    {value: 'ltelte', label: '时间段'},
];

const typeToOption = {
    'TEXT': ['eq', 'neq', 'contains', 'notcontains'],
    'DATE': ['gt', 'gte', 'lt', 'lte', 'ltlt', 'ltlte', 'ltelt', 'ltelte', 'eq', 'neq'],
    'INT': ['gt', 'gte', 'lt', 'lte', 'ltlt', 'ltlte', 'ltelt', 'ltelte', 'eq', 'neq'],
    'BOOLEAN': ['neq', 'eq'],
};

export {
    ruleOptions, propertyDateOption, typeToOption,
};
