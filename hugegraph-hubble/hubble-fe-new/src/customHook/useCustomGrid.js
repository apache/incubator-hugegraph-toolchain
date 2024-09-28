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

/**
 * @file  自定义网格布局
 * @author
 */

import _ from 'lodash';
import G6 from '@antv/g6';
import {useEffect} from 'react';

const options =  {
    small(val) {
        const self = this;
        let res;
        const rows = self.rows;
        const cols = self.cols;
        if (val == null) {
            res = Math.min(rows, cols);
        }
        else {
            const min = Math.min(rows, cols);
            if (min === self.rows) {
                self.rows = val;
            }
            else {
                self.cols = val;
            }
        }
        return res;
    },

    large(val) {
        const self = this;
        let res;
        const rows = self.rows;
        const cols = self.cols;
        if (val == null) {
            res = Math.max(rows, cols);
        }
        else {
            const max = Math.max(rows, cols);
            if (max === self.rows) {
                self.rows = val;
            }
            else {
                self.cols = val;
            }
        }
        return res;
    },

    used(row, col) {
        const self = this;
        return self.cellUsed[`c-${row}-${col}`] || false;
    },

    use(row, col) {
        const self = this;
        self.cellUsed[`c-${row}-${col}`] = true;
    },

    // 依次排完一行后，再往下排 ，固定列数
    moveToNextCell() {
        const self = this;

        const cols = self.cols || 5;
        self.col++;
        if (self.col >= cols) {
            self.col = 0;
            self.row++;
        }

    },

    // 依次排完一列后，再往右排，固定行数
    moveToNextRow() {
        const self = this;
        const rows = self.rows || 5;
        self.row++;
        if (self.row >= rows) {
            self.row = 0;
            self.col++;
        }
    },

    getPos(node) {
        const self = this;
        const begin = [0, 0];
        const cellWidth = self.cellWidth;
        const cellHeight = self.cellHeight;
        let x;
        let y;

        const rcPos = self.id2manPos[node.id];
        if (rcPos) {
            x = rcPos.col * cellWidth + cellWidth / 2 + begin[0];
            y = rcPos.row * cellHeight + cellHeight / 2 + begin[1];
        }
        else {

            while (self.used(self.row, self.col)) {
                if (self.cols > self.rows) {
                    self.moveToNextCell();
                }
                else {
                    self.moveToNextRow();
                }
            }

            x = self.col * cellWidth + cellWidth / 2 + begin[0];
            y = self.row * cellHeight + cellHeight / 2 + begin[1];
            self.use(self.row, self.col);
            if (self.cols > self.rows) {
                self.moveToNextCell();
            }
            else {
                self.moveToNextRow();
            }
        }
        node.x = x;
        node.y = y;
    },

    /**
                * 执行布局
                */
    execute() {
        const self = this;

        const nodes = self.nodes;
        const n = nodes.length;
        const center = self.center;
        const preventOverlap = true;
        if (n === 0) {
            return;
        }
        if (n === 1) {
            nodes[0].x = center[0];
            nodes[0].y = center[1];
            return;
        }

        const edges = self.edges;
        const layoutNodes = [];
        nodes.forEach(node => {
            layoutNodes.push(node);
        });
        const nodeIdxMap = {};
        layoutNodes.forEach((node, i) => {
            nodeIdxMap[node.id] = i;
        });
        // .......其他排序
        // 排序
        layoutNodes.sort((n1, n2) => n2 - n1);

        if (!self.width && typeof window !== 'undefined') {
            self.width = window.innerWidth;
        }
        if (!self.height && typeof window !== 'undefined') {
            self.height = window.innerHeight;
        }

        const oRows = self.rows;
        const oCols = self.cols != null ? self.cols : self.columns;
        self.cells = n;

        // if rows or columns were set in self, use those values
        if (oRows != null && oCols != null) {
            self.rows = oRows;
            self.cols = oCols;
        }
        else if (oRows != null && oCols == null) {
            self.rows = oRows;
            self.cols = Math.ceil(self.cells / self.rows);
        }
        else if (oRows == null && oCols != null) {
            self.cols = oCols;
            self.rows = Math.ceil(self.cells / self.cols);
        }
        else {
            self.splits = Math.sqrt((self.cells * self.height) / self.width);
            self.rows = Math.round(self.splits);
            self.cols = Math.round((self.width / self.height) * self.splits);
        }

        self.cellWidth = self.width / self.cols;
        self.cellHeight = self.height / self.rows;

        if (self.condense) {
            self.cellWidth = 0;
            self.cellHeight = 0;
        }

        if (preventOverlap) {
            layoutNodes.forEach(node => {
                if (!node.x || !node.y) {
                    node.x = 0;
                    node.y = 0;
                }

                let nodew;
                let nodeh;
                if (_.isArray(node.size)) {
                    nodew = node.size[0];
                    nodeh = node.size[1];
                }
                else if (_.isNumber(node.size)) {
                    nodew = node.size;
                    nodeh = node.size;
                }
                if (nodew === undefined || nodeh === undefined) {
                    if (_.isArray(self.nodeSize)) {
                        nodew = self.nodeSize[0];
                        nodeh = self.nodeSize[1];
                    }
                    else if (_.isNumber(self.nodeSize)) {
                        nodew = self.nodeSize;
                        nodeh = self.nodeSize;
                    }
                    else {
                        nodew = 30;
                        nodeh = 30;
                    }
                }

                const p = 30;

                const w = nodew + p;
                const h = nodeh + p;

                self.cellWidth = Math.max(self.cellWidth, w);
                self.cellHeight = Math.max(self.cellHeight, h);
            });
        }

        self.cellUsed = {}; // e.g. 'c-0-2' => true

        self.row = 0;
        self.col = 0;

        self.id2manPos = {};
        for (let i = 0; i < layoutNodes.length; i++) {
            const node = layoutNodes[i];
            let rcPos;
            if (self.position) {
                rcPos = self.position(node);
            }

            if (rcPos && (rcPos.row !== undefined || rcPos.col !== undefined)) {
                const pos = {
                    row: rcPos.row,
                    col: rcPos.col,
                };

                if (pos.col === undefined) {
                    pos.col = 0;
                    while (self.used(pos.row, pos.col)) {
                        pos.col++;
                    }
                }
                else if (pos.row === undefined) {
                    pos.row = 0;

                    while (self.used(pos.row, pos.col)) {
                        pos.row++;
                    }
                }

                self.id2manPos[node.id] = pos;
                self.use(pos.row, pos.col);
            }
            self.getPos(node);
        }
    },
};

const useCustomGrid = () => {
    useEffect(
        () => {
            G6.registerLayout('customGrid', options);
        },
        []
    );
};

export default useCustomGrid;
