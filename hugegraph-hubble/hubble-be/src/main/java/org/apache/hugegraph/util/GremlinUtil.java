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

package org.apache.hugegraph.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableSet;

public final class GremlinUtil {

    private static final Set<String> LIMIT_SUFFIXES = ImmutableSet.of(
            // vertex
            ".V()", ".out()", ".in()", ".both()", ".outV()", ".inV()",
            ".bothV()", ".otherV()",
            // edge
            ".E()", ".outE()", ".inE()", ".bothE()",
            // path
            ".path()", ".simplePath()", ".cyclicPath()",
            // has
            ".hasLabel(STR)", ".hasLabel(NUM)"
    );

    private static final String[] COMPILE_SEARCH_LIST = new String[]{
            ".", "(", ")"
    };
    private static final String[] COMPILE_TARGET_LIST = new String[]{
            "\\.", "\\(", "\\)"
    };

    private static final String[] ESCAPE_SEARCH_LIST = new String[]{
            "\\", "\"", "'", "\n"
    };
    private static final String[] ESCAPE_TARGET_LIST = new String[]{
            "\\\\", "\\\"", "\\'", "\\n"
    };

    private static final Set<Pattern> LIMIT_PATTERNS = compile(LIMIT_SUFFIXES);

    private static final Set<Pattern> IGNORED_PATTERNS = ImmutableSet.of(
            Pattern.compile("^\\s*//.*")
    );

    public static final String GREMLIN_HLM_SCHEMA =
            "graph.schema().propertyKey('姓名').asText().ifNotExist().create();\n" +
            "graph.schema().propertyKey('性别').asText().ifNotExist().create();\n" +
            "graph.schema().propertyKey('年龄').asInt().ifNotExist().create();\n" +
            "graph.schema().propertyKey('特点').asText().ifNotExist().create();\n" +
            "graph.schema().propertyKey('亲疏').asText().ifNotExist().create();\n" +
            "\n" +
            "graph.schema().vertexLabel('男人')" +
            ".properties('姓名', '性别', '年龄', '特点', '亲疏')" +
            ".primaryKeys('姓名').ifNotExist().create();\n" +
            "graph.schema().vertexLabel('女人')" +
            ".properties('姓名', '性别', '年龄', '特点', '亲疏')" +
            ".primaryKeys('姓名').ifNotExist().create();\n" +
            "graph.schema().vertexLabel('机构')" +
            ".properties('姓名', '特点', '亲疏')" +
            ".primaryKeys('姓名').ifNotExist().create();\n" +
            "\n" +
            "graph.schema().edgeLabel('父子').sourceLabel('男人')" +
            ".targetLabel('男人').ifNotExist().create();\n" +
            "graph.schema().edgeLabel('父女').sourceLabel('男人')" +
            ".targetLabel('女人').ifNotExist().create();\n" +
            "graph.schema().edgeLabel('母子').sourceLabel('女人')" +
            ".targetLabel('男人').ifNotExist().create();\n" +
            "graph.schema().edgeLabel('母女').sourceLabel('女人')" +
            ".targetLabel('女人').ifNotExist().create();\n" +
            "graph.schema().edgeLabel('妻').sourceLabel('男人')" +
            ".targetLabel('女人').ifNotExist().create();\n" +
            "graph.schema().edgeLabel('妾').sourceLabel('男人')" +
            ".targetLabel('女人').ifNotExist().create();\n" +
            "graph.schema().edgeLabel('相恋').sourceLabel('男人')" +
            ".targetLabel('女人').ifNotExist().create();\n" +
            "graph.schema().edgeLabel('朋友').sourceLabel('男人')" +
            ".targetLabel('女人').ifNotExist().create();\n" +
            "graph.schema().edgeLabel('姐妹').sourceLabel('女人')" +
            ".targetLabel('女人').ifNotExist().create();\n" +
            "graph.schema().edgeLabel('丫环').sourceLabel('男人')" +
            ".targetLabel('女人').ifNotExist().create();\n" +
            "graph.schema().edgeLabel('丫头').sourceLabel('女人')" +
            ".targetLabel('女人').ifNotExist().create();\n" +
            "graph.schema().edgeLabel('兵部指挥').sourceLabel('机构')" +
            ".targetLabel('男人').ifNotExist().create();\n" +
            "graph.schema().edgeLabel('巡盐御史').sourceLabel('机构')" +
            ".targetLabel('男人').ifNotExist().create();\n" +
            "graph.schema().edgeLabel('尚书令').sourceLabel('机构')" +
            ".targetLabel('男人').ifNotExist().create();\n\n\n";
    public static final String GREMLIN_HLM_DATA =
            "jiataigong = graph.addVertex(T.label, '男人', '姓名', '贾太公', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'o-l5');\n" +
            "jiayan = graph.addVertex(T.label, '男人', '姓名', '贾演', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'o-l6');\n" +
            "jiadaihua = graph.addVertex(T.label, '男人', '姓名', '贾代化', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'o-l5');\n" +
            "jiayuan = graph.addVertex(T.label, '男人', '姓名', '贾源', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'o-l4');\n" +
            "jiadaishan = graph.addVertex(T.label, '男人', '姓名', '贾代善', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'o-l3');\n" +
            "jiamu = graph.addVertex(T.label, '女人', '姓名', '贾母', " +
            "'性别', '女', '年龄', 0, '特点', '史太君，老祖宗', '亲疏', 'o-l3');\n" +
            "jiajing = graph.addVertex(T.label, '男人', '姓名', '贾敬', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'o-l5');\n" +
            "jiazhen = graph.addVertex(T.label, '男人', '姓名', '贾珍', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'm-l4');\n" +
            "youdajie = graph.addVertex(T.label, '女人', '姓名', '尤氏', " +
            "'性别', '女', '年龄', 0, '特点', '-', '亲疏', 'm-l4');\n" +
            "jiarong = graph.addVertex(T.label, '男人', '姓名', '贾蓉', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'y-l3');\n" +
            "qingkeqing = graph.addVertex(T.label, '女人', '姓名', '秦可卿', " +
            "'性别', '女', '年龄', 0, '特点', '-', '亲疏', 'y-l3');\n" +
            "jiashe = graph.addVertex(T.label, '男人', '姓名', '贾赦', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'o-l4');\n" +
            "jialian = graph.addVertex(T.label, '男人', '姓名', '贾琏', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'm-l3');\n" +
            "wangxifeng = graph.addVertex(T.label, '女人', '姓名', '王熙凤', " +
            "'性别', '女', '年龄', 0, '特点', '-', '亲疏', 'm-l2');\n" +
            "youerjie = graph.addVertex(T.label, '女人', '姓名', '尤二姐', " +
            "'性别', '女', '年龄', 0, '特点', '-', '亲疏', 'm-l3');\n" +
            "pinger = graph.addVertex(T.label, '女人', '姓名', '平儿', " +
            "'性别', '女', '年龄', 0, '特点', '-', '亲疏', 'y-l3');\n" +
            "qiutong = graph.addVertex(T.label, '女人', '姓名', '秋桐', " +
            "'性别', '女', '年龄', 0, '特点', '-', '亲疏', 'y-l3');\n" +
            "jiayingchun = graph.addVertex(T.label, '女人', '姓名', '贾迎春', " +
            "'性别', '女', '年龄', 0, '特点', '-', '亲疏', 'y-l3');\n" +
            "sunshaozu = graph.addVertex(T.label, '男人', '姓名', '孙绍祖', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'y-l3');\n" +
            "jiazheng = graph.addVertex(T.label, '男人', '姓名', '贾政', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'm-l2');\n" +
            "wangfuren = graph.addVertex(T.label, '女人', '姓名', '王夫人', " +
            "'性别', '女', '年龄', 0, '特点', '-', '亲疏', 'm-l2');\n" +
            "zhaoyiniang = graph.addVertex(T.label, '女人', '姓名', '赵姨娘', " +
            "'性别', '女', '年龄', 0, '特点', '-', '亲疏', 'm-l4');\n" +
            "jiahuan = graph.addVertex(T.label, '男人', '姓名', '贾环', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'y-l4');\n" +
            "jiatanchun = graph.addVertex(T.label, '女人', '姓名', '贾探春', " +
            "'性别', '女', '年龄', 0, '特点', '-', '亲疏', 'y-l4');\n" +
            "jiayuanchun = graph.addVertex(T.label, '女人', '姓名', '贾元春', " +
            "'性别', '女', '年龄', 0, '特点', '-', '亲疏', 'y-l3');\n" +
            "jiazhu = graph.addVertex(T.label, '男人', '姓名', '贾珠', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'y-l3');\n" +
            "liwan = graph.addVertex(T.label, '女人', '姓名', '李纨', " +
            "'性别', '女', '年龄', 0, '特点', '-', '亲疏', 'm-l3');\n" +
            "jialan = graph.addVertex(T.label, '男人', '姓名', '贾兰', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'b-l4');\n" +
            "jiabaoyu = graph.addVertex(T.label, '男人', '姓名', '贾宝玉', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'y-l1');\n" +
            "xuebaochai = graph.addVertex(T.label, '女人', '姓名', '薛宝钗', " +
            "'性别', '女', '年龄', 0, '特点', '-', '亲疏', 'y-l2');\n" +
            "jiamin = graph.addVertex(T.label, '女人', '姓名', '贾敏', " +
            "'性别', '女', '年龄', 0, '特点', '-', '亲疏', 'm-l3');\n" +
            "linruhai = graph.addVertex(T.label, '男人', '姓名', '林如海', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'm-l3');\n" +
            "lindaiyu = graph.addVertex(T.label, '女人', '姓名', '林黛玉', " +
            "'性别', '女', '年龄', 0, '特点', '-', '亲疏', 'y-l1');\n" +
            "shihou = graph.addVertex(T.label, '男人', '姓名', '史候', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'o-l6');\n" +
            "shigong = graph.addVertex(T.label, '男人', '姓名', '史公', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'o-l5');\n" +
            "shiba = graph.addVertex(T.label, '男人', '姓名', '史氏', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'o-l4');\n" +
            "shinai = graph.addVertex(T.label, '男人', '姓名', '史鼐', " +
            "'性别', '男', '年龄', 0, '特点', '保龄侯', '亲疏', 'o-l4');\n" +
            "shiding = graph.addVertex(T.label, '男人', '姓名', '史鼎', " +
            "'性别', '男', '年龄', 0, '特点', '忠靖侯', '亲疏', 'o-l4');\n" +
            "shixiangyun = graph.addVertex(T.label, '女人', '姓名', '史湘云', " +
            "'性别', '女', '年龄', 0, '特点', '-', '亲疏', 'y-l3');\n" +
            "weiruolan = graph.addVertex(T.label, '男人', '姓名', '卫若兰', " +
            "'性别', '男', '年龄', 0, '特点', '-', '亲疏', 'm-l4');\n" +
            "xueyima = graph.addVertex(T.label, '女人', '姓名', '薛姨妈', " +
            "'性别', '女', '年龄', 0, '特点', '-', '亲疏', 'm-l3');\n" +
            "\n" +
            "jiataigong.addEdge('父子', jiayan);\n" +
            "jiataigong.addEdge('父子', jiayuan);\n" +
            "jiayan.addEdge('父子', jiadaihua);\n" +
            "jiayuan.addEdge('父子', jiadaishan);\n" +
            "jiadaishan.addEdge('妻', jiamu);\n" +
            "jiadaihua.addEdge('父子', jiajing);\n" +
            "jiajing.addEdge('父子', jiazhen);\n" +
            "jiazhen.addEdge('父子', jiarong);\n" +
            "jiazhen.addEdge('妾', youdajie);\n" +
            "jiarong.addEdge('妻', qingkeqing);\n" +
            "jiashe.addEdge('父子', jialian);\n" +
            "jialian.addEdge('妻', wangxifeng);\n" +
            "jialian.addEdge('妾', youerjie);\n" +
            "jialian.addEdge('妾', pinger);\n" +
            "jialian.addEdge('妾', qiutong);\n" +
            "wangxifeng.addEdge('丫头', pinger);\n" +
            "jiashe.addEdge('丫环', qiutong);\n" +
            "youdajie.addEdge('姐妹', youerjie);\n" +
            "jiashe.addEdge('父女', jiayingchun);\n" +
            "sunshaozu.addEdge('妻', jiayingchun);\n" +
            "jiazheng.addEdge('妻', wangfuren);\n" +
            "jiazheng.addEdge('妾', zhaoyiniang);\n" +
            "zhaoyiniang.addEdge('母子', jiahuan);\n" +
            "zhaoyiniang.addEdge('母女', jiatanchun);\n" +
            "wangfuren.addEdge('母女', jiayuanchun);\n" +
            "wangfuren.addEdge('母子', jiazhu);\n" +
            "jiazhu.addEdge('妻', liwan);\n" +
            "jiazhu.addEdge('父子', jialan);\n" +
            "liwan.addEdge('母子', jialan);\n" +
            "wangfuren.addEdge('母子', jiabaoyu);\n" +
            "jiabaoyu.addEdge('妻', xuebaochai);\n" +
            "linruhai.addEdge('妻', jiamin);\n" +
            "linruhai.addEdge('父女', lindaiyu);\n" +
            "jiamin.addEdge('母女', lindaiyu);\n" +
            "jiabaoyu.addEdge('相恋', lindaiyu);\n" +
            "jiamu.addEdge('母子', jiashe);\n" +
            "jiamu.addEdge('母子', jiazheng);\n" +
            "jiamu.addEdge('母女', jiamin);\n" +
            "jiadaishan.addEdge('父子', jiashe);\n" +
            "jiadaishan.addEdge('父子', jiazheng);\n" +
            "jiadaishan.addEdge('父女', jiamin);\n" +
            "shihou.addEdge('父子', shigong);\n" +
            "shigong.addEdge('父子', shiba);\n" +
            "shigong.addEdge('父子', shinai);\n" +
            "shigong.addEdge('父子', shiding);\n" +
            "shigong.addEdge('父女', jiamu);\n" +
            "shiba.addEdge('父女', shixiangyun);\n" +
            "weiruolan.addEdge('妻', shixiangyun);\n" +
            "jiabaoyu.addEdge('朋友', shixiangyun);\n" +
            "xueyima.addEdge('姐妹', wangfuren);\n" +
            "xueyima.addEdge('母女', xuebaochai);";

    public static final String GREMLIN_LOAD_HLM = GREMLIN_HLM_SCHEMA +
                                                  GREMLIN_HLM_DATA;

    public static final String GREMLIN_COVID19_SCHEMA =
            "\"graph.schema().propertyKey('性别').asText().ifNotExist().create();\\n" +
            "graph.schema().propertyKey('年龄').asInt().ifNotExist().create();\\n" +
            "graph.schema().propertyKey('防疫政策').asText().ifNotExist().create();\\n" +
            "graph.schema().propertyKey('类型').asText().ifNotExist().create();\\n" +
            "graph.schema().propertyKey('时间').asText().ifNotExist().create();\\n" +
            "\\n" +
            "graph.schema().vertexLabel('patient').properties('性别','年龄')" +
            ".useCustomizeStringId().nullableKeys('性别','年龄')" +
            ".enableLabelIndex(false).ifNotExist().create();\\n" +
            "graph.schema().vertexLabel('place')" +
            ".useCustomizeStringId()" +
            ".enableLabelIndex(false).ifNotExist().create();\\n" +
            "graph.schema().vertexLabel('city').properties('防疫政策')" +
            ".useCustomizeStringId().nullableKeys('防疫政策')" +
            ".enableLabelIndex(false).ifNotExist().create();\\n" +
            "graph.schema().vertexLabel('vehicle').properties('类型')" +
            ".useCustomizeStringId().nullableKeys('类型')" +
            ".enableLabelIndex(false).ifNotExist().create();\\n" +
            "\\n" +
            "graph.schema().edgeLabel('relation')" +
            ".sourceLabel('patient').targetLabel('patient')" +
            ".properties('类型').nullableKeys('类型')" +
            ".enableLabelIndex(false).ifNotExist().create();\\n" +
            "graph.schema().edgeLabel('reside')" +
            ".sourceLabel('patient').targetLabel('place')" +
            ".properties('时间').nullableKeys('时间')" +
            ".enableLabelIndex(false).ifNotExist().create();\\n" +
            "graph.schema().edgeLabel('work')" +
            ".sourceLabel('patient').targetLabel('place')" +
            ".properties('时间').nullableKeys('时间')" +
            ".enableLabelIndex(false).ifNotExist().create();\\n" +
            "graph.schema().edgeLabel('stay')" +
            ".sourceLabel('patient').targetLabel('place')" +
            ".properties('时间').nullableKeys('时间')" +
            ".enableLabelIndex(false).ifNotExist().create();" +
            "\\ngraph.schema().edgeLabel('comfirm')" +
            ".sourceLabel('patient').targetLabel('city')" +
            ".properties('时间').nullableKeys('时间')" +
            ".enableLabelIndex(false).ifNotExist().create();\\n" +
            "graph.schema().edgeLabel('take')" +
            ".sourceLabel('patient').targetLabel('vehicle')" +
            ".properties('时间').nullableKeys('时间')" +
            ".enableLabelIndex(false).ifNotExist().create();\\n\\n\"";

    public static String escapeId(Object id) {
        if (!(id instanceof String)) {
            return id.toString();
        }
        String text = (String) id;
        text = StringUtils.replaceEach(text, ESCAPE_SEARCH_LIST,
                                       ESCAPE_TARGET_LIST);
        return (String) escape(text);
    }

    public static Object escape(Object object) {
        if (!(object instanceof String)) {
            return object;
        }
        return StringUtils.wrap((String) object, '\'');
    }

    public static String optimizeLimit(String gremlin, int limit) {
        String[] rawLines = StringUtils.split(gremlin, "\n");
        List<String> newLines = new ArrayList<>(rawLines.length);
        for (String rawLine : rawLines) {
            boolean ignored = IGNORED_PATTERNS.stream().anyMatch(pattern -> {
                return pattern.matcher(rawLine).find();
            });
            if (ignored) {
                newLines.add(rawLine);
                continue;
            }
            boolean matched = false;
            for (Pattern pattern : LIMIT_PATTERNS) {
                Matcher matcher = pattern.matcher(rawLine);
                if (matcher.find()) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                newLines.add(rawLine + ".limit(" + limit + ")");
            } else {
                newLines.add(rawLine);
            }
        }
        return StringUtils.join(newLines, "\n");
    }

    private static Set<Pattern> compile(Set<String> texts) {
        Set<Pattern> patterns = new LinkedHashSet<>();
        for (String text : texts) {
            String regex = StringUtils.replaceEach(text, COMPILE_SEARCH_LIST,
                                                   COMPILE_TARGET_LIST);
            Pattern pattern;
            // Assume that (STR), (NUM) and () not exist at the same time
            if (text.contains("(STR)")) {
                // single quote
                pattern = compile(regex.replaceAll("STR", "'[\\\\s\\\\S]+'"));
                patterns.add(pattern);
                // double quotes
                pattern = compile(regex.replaceAll("STR", "\"[\\\\s\\\\S]+\""));
                patterns.add(pattern);
            } else if (text.contains("(NUM)")) {
                pattern = compile(regex.replaceAll("NUM", "[\\\\d]+"));
                patterns.add(pattern);
            } else if (text.contains("()")) {
                pattern = compile(regex);
                patterns.add(pattern);
            }
        }
        return patterns;
    }

    private static Pattern compile(String regex) {
        String finalRegex = "(" + regex + ")$";
        return Pattern.compile(finalRegex);
    }
}
