const byteConvert = bytes => {
    if (isNaN(bytes)) {
        return '';
    }
    // let symbols = ['bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
    let symbols = ['KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
    let exp = Math.floor(Math.log(bytes) / Math.log(2));
    if (exp < 1) {
        exp = 0;
    }
    let i = Math.floor(exp / 10);
    bytes = bytes / Math.pow(2, 10 * i);

    if (bytes.toString().length > bytes.toFixed(2).toString().length) {
        bytes = bytes.toFixed(2);
    }
    return bytes + ' ' + symbols[i];
};

const timeConvert = seconds => {
    if (isNaN(seconds)) {
        return '';
    }
    seconds = Math.max(seconds, 1);

    if (seconds < 60) {
        return `${seconds}s`;
    }

    if (seconds < 3600) {
        seconds = (seconds / 60).toFixed(1);
        return `${seconds}min`;
    }

    seconds = (seconds / 3600).toFixed(1);

    return `${seconds}h`;
};

export {byteConvert, timeConvert};
