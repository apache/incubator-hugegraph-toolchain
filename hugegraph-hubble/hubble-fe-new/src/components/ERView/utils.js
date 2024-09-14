import {useTranslation} from 'react-i18next';

const setPropertyRow = item => {
    return {...item, attr: {...item.attr, rect: {fill: '#FAFAFA'}}};
};

const setValueRow = item => {
    return {...item, attr: {...item.attr, rect: {fill: '#ACACAC'}}};
};

const setCell = (t, cell, type) => {
    const idList = [];
    if (type === 'edge') {
        idList.push(
            {
                id: `${cell.name}-source`,
                group: 'list',
                attrs: {
                    portNameLabel: {
                        text: t('ERView.edge.start'),
                    },
                    portTypeLabel: {
                        text: '',
                    },
                    rect: {
                        fill: '#FAFAFA',
                    },
                },
            }
        );

        idList.push(
            {
                id: `${cell.name}-target`,
                group: 'list',
                attrs: {
                    portNameLabel: {
                        text: t('ERView.edge.end'),
                    },
                    portTypeLabel: {
                        text: '',
                    },
                    rect: {
                        fill: '#FAFAFA',
                    },
                },
            }
        );
    }
    else {
        idList.push(
            {
                id: `${cell.name}-ID`,
                group: 'list',
                attrs: {
                    portNameLabel: {
                        text: 'ID',
                    },
                    portTypeLabel: {
                        text: '',
                    },
                    rect: {
                        fill: '#FAFAFA',
                    },
                },
            }
        );
    }

    const propertyList = cell.properties.map(item => ({
        id: `${cell.name}-${item.name}`,
        group: 'list',
        attrs: {
            portNameLabel: {
                text: 'Property',
            },
            portTypeLabel: {
                text: item.name,
            },
        },
    }));

    return {
        id: cell.name,
        shape: 'er-rect',
        label: cell.name,
        width: 150,
        height: 24,
        position: {
            x: 4,
            y: 150,
        },
        ports: [
            ...idList,
            ...propertyList,
        ],
    };
};

export {setPropertyRow, setValueRow, setCell};
