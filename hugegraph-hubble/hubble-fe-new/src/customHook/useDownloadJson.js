/**
 * @file  下载Json数据
 * @author
 */

const useDownloadJson = () => {

    const downloadJsonHandler = (fileName, data) => {
        const formatedFileName = fileName.split('.').join('');
        let element = document.createElement('a');
        const processedData = JSON.stringify(data);
        element.setAttribute('href',
            `data:application/json;charset=utf-8,\ufeff${encodeURIComponent(processedData)}`);
        element.setAttribute('download', formatedFileName);
        element.style.display = 'none';
        document.body.appendChild(element);
        element.click();
        document.body.removeChild(element);
    };

    return {downloadJsonHandler};
};

export default useDownloadJson;