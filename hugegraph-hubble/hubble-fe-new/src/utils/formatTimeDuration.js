import {intervalToDuration, formatDuration} from 'date-fns';
import _ from 'lodash';

/**
 * 转换时间数据格式
 * @param {*} duration
 * @returns
 */

export default function formatTimeDuration(start, end) {
    const duration = intervalToDuration({start, end});
    const {days, hours, minutes, seconds} = duration || {};
    const preFormatedDuration = formatDuration(duration, {format: ['days', 'hours', 'minutes', 'seconds']});

    const replaceMap = {
        ' days': 'd',
        ' day': 'd',
        ' hours': 'h',
        ' hour': 'h',
        ' minutes': 'm',
        ' minute': 'm',
        ' seconds': 's',
        ' second': 's',
    };

    let formatedDuration = preFormatedDuration;
    Object.keys(replaceMap).forEach(
        key => {
            formatedDuration = _.replace(formatedDuration, key, replaceMap[key]);
        }
    );
    const restMilliSecond = end - start - seconds * 1000 - minutes * 1000 * 60
    - hours * 1000 * 60 * 60 - days * 1000 * 60 * 60 * 24;

    formatedDuration = formatedDuration + ` ${restMilliSecond}ms`;
    return formatedDuration;
}