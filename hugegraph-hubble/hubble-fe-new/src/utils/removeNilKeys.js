import _ from 'lodash';

export default function removeNilKeys(obj) {
    if (_.isString(obj)) {
        return sanitizeString(obj);
    }
    if (_.isArray(obj)) {
        return sanitizeArray(obj);
    }
    if (_.isPlainObject(obj)) {
        return sanitizeObject(obj);
    }
    return obj;
}

function isProvided(value) {
    const typeIsNotSupported = !_.isNil(value) && !_.isString(value)
    && !_.isArray(value) && !_.isPlainObject(value);
    return typeIsNotSupported || !_.isEmpty(value);
}

function transToNumber(val) {
    const matchVal = val.match(/\`(.*)\`/);

    return matchVal ? Number(matchVal[1]) : val;
}

function sanitizeString(str) {
    return _.isEmpty(str) ? null : transToNumber(str);
}

function sanitizeArray(arr) {
    return _.filter(_.map(arr, removeNilKeys), isProvided);
}

function sanitizeObject(obj) {
    return _.pickBy(_.mapValues(obj, removeNilKeys), isProvided);
}

