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

import enAnalysis from '../../i18n/resources/en-US/modules/analysis.json';
import {
    EDGELABEL_TYPE_NAME,
} from '../../utils/constants';

const get = path => path.split('.').reduce((value, key) => value && value[key], enAnalysis);
const hasChinese = value => /[\u4e00-\u9fff]/.test(value);

describe('canvas i18n coverage', () => {
    it('provides English labels for graph canvas panels used by algorithm pages', () => {
        const requiredKeys = [
            'analysis.canvas.canvas_3d.node',
            'analysis.canvas.canvas_3d.edge',
            'analysis.canvas.canvas_3d.tip',
            'analysis.canvas.toolbar.clear_canvas',
            'analysis.canvas.toolbar.fit_center',
            'analysis.canvas.toolbar.full_screen',
            'analysis.canvas.toolbar.undo',
            'analysis.canvas.toolbar.redo',
            'analysis.canvas.toolbar.refresh_layout',
            'analysis.canvas.toolbar.zoom_out',
            'analysis.canvas.toolbar.zoom_in',
            'analysis.canvas.toolbar.fix_node',
            'analysis.canvas.toolbar.isolated_nodes',
            'analysis.canvas.clear_graph.title',
            'analysis.canvas.clear_graph.description',
            'analysis.canvas.clear_graph.ok',
            'analysis.canvas.clear_graph.cancel',
            'analysis.canvas.context_menu.expand',
            'analysis.canvas.context_menu.search',
            'analysis.canvas.context_menu.fix',
            'analysis.canvas.context_menu.unfix',
            'analysis.canvas.context_menu.hide',
            'analysis.canvas.context_menu.add_out_edge',
            'analysis.canvas.context_menu.add_in_edge',
            'analysis.canvas.context_menu.add_vertex',
            'analysis.canvas.filter_drawer.logic',
            'analysis.canvas.filter_drawer.filter_type',
            'analysis.canvas.filter_drawer.operation_type',
            'analysis.canvas.filter_drawer.vertex',
            'analysis.canvas.filter_drawer.edge',
            'analysis.canvas.filter_drawer.vertex_property',
            'analysis.canvas.filter_drawer.vertex_id',
            'analysis.canvas.filter_drawer.vertex_label',
            'analysis.canvas.filter_drawer.edge_property',
            'analysis.canvas.filter_drawer.edge_id',
            'analysis.canvas.filter_drawer.edge_label',
            'analysis.canvas.filter_drawer.add_expression',
            'analysis.canvas.filter_drawer.delete_expression',
            'analysis.canvas.legend.title',
            'analysis.canvas.element_tooltip.node',
            'analysis.canvas.element_tooltip.edge',
            'analysis.canvas.element_tooltip.type',
            'analysis.canvas.element_tooltip.edge_label_type.parent',
            'analysis.canvas.element_tooltip.edge_label_type.sub',
            'analysis.canvas.element_tooltip.edge_label_type.normal',
            'analysis.canvas.task_navigation.success_alt',
            'analysis.canvas.task_navigation.task_id',
            'analysis.canvas.task_navigation.view',
            'analysis.canvas.filter_drawer.title',
            'analysis.canvas.filter_drawer.add_property',
            'analysis.canvas.setting_panel.title',
            'analysis.canvas.layout_panel.title',
            'analysis.canvas.layout_panel.force',
            'analysis.canvas.statistics_panel.label_statistics',
            'analysis.canvas.statistics_panel.graph_statistics',
            'analysis.canvas.statistics_panel.highlight',
            'analysis.canvas.dynamic_add.vertex_type',
            'analysis.canvas.edit_element.edit_details',
        ];

        requiredKeys.forEach(key => {
            const value = get(key);

            expect(typeof value).toBe('string');
            expect(value.length).toBeGreaterThan(0);
            expect(hasChinese(value)).toBe(false);
        });
    });

    it('keeps edge label type display constants as i18n keys', () => {
        Object.values(EDGELABEL_TYPE_NAME).forEach(value => {
            expect(value).toMatch(/^analysis\.canvas\.element_tooltip\.edge_label_type\./);
            expect(hasChinese(value)).toBe(false);
            expect(get(value)).toBeTruthy();
            expect(hasChinese(get(value))).toBe(false);
        });
    });
});
