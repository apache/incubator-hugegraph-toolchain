const LINE_HEIGHT = 24;
const NODE_WIDTH = 150;

const erRectConfig = {
    inherit: 'rect',
    markup: [
        {
            tagName: 'rect',
            selector: 'body',
        },
        {
            tagName: 'text',
            selector: 'label',
        },
        // {
        //     tagName: 'g',
        //     attrs: {
        //         class: 'btn add',
        //     },
        //     children: [
        //         {
        //             tagName: 'circle',
        //             attrs: {
        //                 class: 'add',
        //             },
        //         },
        //         {
        //             tagName: 'text',
        //             attrs: {
        //                 class: 'add',
        //             },
        //         },
        //     ],
        // },
    ],
    tools: [
        {
            name: 'button',
            args: {
                x: 10,
                y: 10,
                markup: [
                    // {
                    //     tagName: 'circle',
                    //     attrs: {
                    //         stroke: '#fff',
                    //         fill: 'transparent',
                    //         cursor: 'pointer',
                    //         'stroke-width': 1,
                    //         r: 8,
                    //     },
                    // },
                    {
                        tagName: 'text',
                        textContent: '∅',
                        attrs: {
                            fontSize: 16,
                            fontWeight: 800,
                            x: -4,
                            y: 6,
                            fill: '#fff',
                            fontFamily: 'Times New Roman',
                            cursor: 'pointer',
                        },
                    },
                ],
            },
        },
        {
            name: 'button-remove',
            args: {
                x: '100%',
                y: 0,
                offset: {x: 0, y: 0},
            },
        },
    ],
    attrs: {
        rect: {
            strokeWidth: 1,
            stroke: '#5F95FF',
            fill: '#5F95FF',
        },
        label: {
            fontWeight: 'bold',
            fill: '#ffffff',
            fontSize: 12,
        },
        '.btn.add': {
            'refDx': -16,
            'refY': 12,
            'event': 'node:add',
        },
        '.btn.del': {
            'refDx': -44,
            'refY': 16,
            'event': 'node:delete',
        },
        '.btn > circle': {
            'r': 8,
            'fill': 'transparent',
            'stroke': '#fff',
            'strokeWidth': 1,
        },
        '.btn.add > text': {
            'fontSize': 16,
            'fontWeight': 800,
            'fill': '#fff',
            'x': -75,
            'y': -12,
            'fontFamily': 'Times New Roman',
            'text': '+',
        },
        '.btn.del > text': {
            'fontSize': 28,
            'fontWeight': 500,
            'fill': '#fff',
            'x': -4.5,
            'y': 6,
            'fontFamily': 'Times New Roman',
            'text': '-',
        },
    },
    ports: {
        groups: {
            list: {
                markup: [
                    {
                        tagName: 'rect',
                        selector: 'portBody',
                    },
                    {
                        tagName: 'text',
                        selector: 'portNameLabel',
                    },
                    {
                        tagName: 'text',
                        selector: 'portTypeLabel',
                    },
                ],
                attrs: {
                    portBody: {
                        width: NODE_WIDTH,
                        height: LINE_HEIGHT,
                        strokeWidth: 1,
                        stroke: '#5F95FF',
                        fill: '#EFF4FF',
                        magnet: true,
                    },
                    portNameLabel: {
                        ref: 'portBody',
                        refX: 6,
                        refY: 6,
                        fontSize: 10,
                    },
                    portTypeLabel: {
                        ref: 'portBody',
                        refX: 95,
                        refY: 6,
                        fontSize: 10,
                    },
                },
                position: 'erPortPosition',
            },
            title: {
                markup: [
                    {
                        tagName: 'rect',
                        selector: 'portBody',
                    },
                    {
                        tagName: 'text',
                        selector: 'portNameLabel',
                    },
                    {
                        tagName: 'text',
                        selector: 'portTypeLabel',
                    },
                ],
                attrs: {
                    portBody: {
                        width: NODE_WIDTH,
                        height: LINE_HEIGHT,
                        strokeWidth: 1,
                        stroke: '#5F95FF',
                        fill: '#FFFF00',
                        magnet: true,
                    },
                    portNameLabel: {
                        ref: 'portBody',
                        refX: 6,
                        refY: 6,
                        fontSize: 10,
                    },
                    portTypeLabel: {
                        ref: 'portBody',
                        refX: 95,
                        refY: 6,
                        fontSize: 10,
                    },
                },
                position: 'erPortPosition',
            },
        },
    },
};

const erRectHeadConfig = {
    inherit: 'rect',
    markup: [
        {
            tagName: 'rect',
            selector: 'body',
        },
        {
            tagName: 'text',
            selector: 'label',
        },
    ],
    attrs: {
        rect: {
            strokeWidth: 1,
            stroke: '#5F95FF',
            fill: '#5F95FF',
        },
        label: {
            fontWeight: 'bold',
            fill: '#ffffff',
            fontSize: 12,
        },
    },
    ports: {
        groups: {
            list: {
                markup: [
                    {
                        tagName: 'rect',
                        selector: 'portBody',
                    },
                    {
                        tagName: 'text',
                        selector: 'portNameLabel',
                    },
                    {
                        tagName: 'text',
                        selector: 'portTypeLabel',
                    },
                ],
                attrs: {
                    portBody: {
                        width: NODE_WIDTH,
                        height: LINE_HEIGHT,
                        strokeWidth: 1,
                        stroke: '#5F95FF',
                        fill: '#EFF4FF',
                        magnet: true,
                    },
                    portNameLabel: {
                        ref: 'portBody',
                        refX: 6,
                        refY: 6,
                        fontSize: 10,
                    },
                    portTypeLabel: {
                        ref: 'portBody',
                        refX: 95,
                        refY: 6,
                        fontSize: 10,
                    },
                },
                position: 'erPortPosition',
            },
        },
    },
};

const erPortPosition = portsPositionArgs => {
    return portsPositionArgs.map((a, index) => {
        return {
            position: {
                x: 0,
                y: (index + 1) * LINE_HEIGHT,
            },
            angle: 0,
        };
    });
};

export {erRectConfig, erRectHeadConfig, erPortPosition};
