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

import moment from 'moment';

// 适用 number, date, text
const checkEq = (left, right, type) => {
    if (type === 'DATE') {
        return moment(left).isSame(right.format('YYYY-MM-DD'));
    }

    if (type === 'INT') {
        return Number(left) === Number(right);
    }

    return left === right;
};

const checkNeq = (left, right, type) => {
    if (type === 'DATE') {
        return !moment(left).isSame(right.format('YYYY-MM-DD'));
    }

    if (type === 'INT') {
        return Number(left) !== Number(right);
    }

    return left !== right;
};

const checkGt = (left, right, type) => {
    if (type === 'DATE') {
        return moment(left).isAfter(right.format('YYYY-MM-DD'));
    }

    if (type === 'INT') {
        return Number(left) > Number(right);
    }

    return left > right;
};

const checkGte = (left, right, type) => {
    if (type === 'DATE') {
        return moment(left).isSameOrAfter(right.format('YYYY-MM-DD'));
    }

    if (type === 'INT') {
        return Number(left) >= Number(right);
    }

    return left >= right;
};

const checkLt = (left, right, type) => {
    if (type === 'DATE') {
        return moment(left).isBefore(right.format('YYYY-MM-DD'));
    }

    if (type === 'INT') {
        return Number(left) < Number(right);
    }

    return left < right;
};

const checkLte = (left, right, type) => {
    if (type === 'DATE') {
        return moment(left).isSameOrBefore(right.format('YYYY-MM-DD'));
    }

    if (type === 'INT') {
        return Number(left) <= Number(right);
    }

    return left <= right;
};

const checkLtlt = (left, right, type) => {
    if (type === 'DATE') {
        return moment(left).isBetween(right[0].format('YYYY-MM-DD'), right[1].format('YYYY-MM-DD'), null, '()');
    }

    if (type === 'INT') {
        return Number(left) > Number(right[0]) && Number(left) < Number(right[1]);
    }

    return left > right[0] && left < right[1];
};

const checkLtelt = (left, right, type) => {
    if (type === 'DATE') {
        return moment(left).isBetween(right[0].format('YYYY-MM-DD'), right[1].format('YYYY-MM-DD'), null, '[)');
    }

    if (type === 'INT') {
        return Number(left) >= Number(right[0]) && Number(left) < Number(right[1]);
    }

    return left >= right[0] && left < right[1];
};

const checkLtelte = (left, right, type) => {
    if (type === 'DATE') {
        return moment(left).isBetween(right[0].format('YYYY-MM-DD'), right[1].format('YYYY-MM-DD'), null, '[]');
    }

    if (type === 'INT') {
        return Number(left) >= Number(right[0]) && Number(left) <= Number(right[1]);
    }

    return left >= right[0] && left <= right[1];
};

const checkLtlte = (left, right, type) => {
    if (type === 'DATE') {
        return moment(left).isBetween(right[0].format('YYYY-MM-DD'), right[1].format('YYYY-MM-DD'), null, '(]');
    }

    if (type === 'INT') {
        return Number(left) > Number(right[0]) && Number(left) <= Number(right[1]);
    }

    return left > right[0] && left <= right[1];
};

const checkRule = (left, right, rule, data_type) => {
    // 类型转换
    // if (((Array.isArray(right) && )

    if (rule === 'eq') {
        // return typeof left === 'number' ? left === Number(right) : left === right;
        return checkEq(left, right, data_type);
    }

    if (rule === 'neq') {
        // return typeof left === 'number' ? left !== Number(right) : left !== right;
        return checkNeq(left, right, data_type);
    }

    if (rule === 'gt') {
        // return left > right;
        return checkGt(left, right, data_type);
    }

    if (rule === 'gte') {
        // return left >= right;
        return checkGte(left, right, data_type);
    }

    if (rule === 'lt') {
        // return left < right;
        return checkLt(left, right, data_type);
    }

    if (rule === 'lte') {
        // return left <= right;
        return checkLte(left, right, data_type);
    }

    if (rule === 'ltlt') {
        // return left < right[0] && left < right[1];
        return checkLtlt(left, right, data_type);
    }

    if (rule === 'ltelt') {
        // return left <= right[0] && left < right[1];
        return checkLtelt(left, right, data_type);
    }

    if (rule === 'ltlte') {
        // return left < right[0] && left <= right[1];
        return checkLtlte(left, right, data_type);
    }

    if (rule === 'ltelte') {
        // return left <= right[0] && left <= right[1];
        return checkLtelte(left, right, data_type);
    }

    if (rule === 'contains') {
        return new RegExp(right).test(left);
    }

    if (rule === 'notcontains') {
        return !(new RegExp(right).test(left));
    }
};

const filterCondition = (node, rules, logic) => {
    let flag = '';

    if (rules.length === 0) {
        return true;
    }

    for (let rule of rules) {
        if (rule.op_type === 'id') {
            // flag = checkRule(node.id.split(':')[1], rule.value, rule.rule);
            flag = checkRule(node.id, rule.value, rule.rule, rule.data_type);
        }

        if (rule.op_type === 'label') {
            flag = checkRule(node.label, rule.value, rule.rule, rule.data_type);
        }

        if (rule.op_type === 'property') {
            flag = checkRule(node.properties[rule.property], rule.value, rule.rule, rule.data_type);
        }

        // or 条件下一个为true结果为true, and 条件下一个为false结果为false
        if (logic === 'and' && flag === false) {
            return false;
        }

        if (logic === 'or' && flag === true) {
            return true;
        }
    }

    return flag;
};

const filterData = (dataList, rules, logic) => {
    const filterVertex = [];
    const filterEdge = [];
    const {vertices, edges} = dataList;

    if (rules.vertex.length === 0 && rules.edge.length === 0) {
        return dataList;
    }

    for (let k in vertices) {
        if ({}.hasOwnProperty.call(vertices, k)) {
            const vertex = vertices[k];
            if (filterCondition(vertex, rules.vertex, logic)) {
                filterVertex.push(vertex);
            }
        }
    }

    const vertexList = filterVertex.map(item => item.id);

    for (let k in edges) {
        if ({}.hasOwnProperty.call(edges, k)) {
            const edge = edges[k];
            if (!vertexList.includes(edge.source) || !vertexList.includes(edge.target)) {
                continue;
            }
            if (filterCondition(edge, rules.edge, logic)) {
                filterEdge.push(edge);
            }
        }
    }

    return {vertices: filterVertex, edges: filterEdge};
};

const expandData = (dataList, newData) => {
    return {vertices: [...dataList.vertices, ...newData.vertices], edges: [...dataList.edges, ...newData.edges]};
};

export {filterData, expandData};
