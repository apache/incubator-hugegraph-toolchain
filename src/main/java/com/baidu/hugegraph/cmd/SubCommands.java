/*
 * Copyright 2017 HugeGraph Authors
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

package com.baidu.hugegraph.cmd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.baidu.hugegraph.api.API;
import com.baidu.hugegraph.driver.TaskManager;
import com.baidu.hugegraph.manager.TasksManager;
import com.baidu.hugegraph.structure.constant.GraphMode;
import com.baidu.hugegraph.structure.constant.HugeType;
import com.baidu.hugegraph.util.E;
import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class SubCommands {

    private Map<String, Object> commands;

    public SubCommands() {
        this.commands = new HashMap<>();
        this.initSubCommands();
    }

    private void initSubCommands() {
        this.commands.put("backup", new Backup());
        this.commands.put("restore", new Restore());
        this.commands.put("schedule-backup", new ScheduleBackup());
        this.commands.put("dump", new DumpGraph());
        this.commands.put("graph-list", new GraphList());
        this.commands.put("graph-get", new GraphGet());
        this.commands.put("graph-clear", new GraphClear());
        this.commands.put("graph-mode-set", new GraphModeSet());
        this.commands.put("graph-mode-get", new GraphModeGet());
        this.commands.put("gremlin-execute", new Gremlin());
        this.commands.put("gremlin-schedule", new GremlinJob());
        this.commands.put("task-list", new TaskList());
        this.commands.put("task-get", new TaskGet());
        this.commands.put("task-delete", new TaskDelete());
        this.commands.put("task-cancel", new TaskCancel());
        this.commands.put("task-clear", new TaskClear());
        this.commands.put("deploy", new Deploy());
        this.commands.put("start-all", new StartAll());
        this.commands.put("clear", new Clear());
        this.commands.put("stop-all", new StopAll());
        this.commands.put("help", new Help());
    }

    public Map<String, Object> commands() {
        return this.commands;
    }

    @Parameters(commandDescription = "Schedule backup task")
    public class ScheduleBackup {

        @Parameter(names = {"--interval"}, arity = 1,
                   description =
                           "The interval of backup, format is: \"a b c d e\"." +
                           " 'a' means minute (0 - 59)," +
                           " 'b' means hour (0 - 23)," +
                           " 'c' means day of month (1 - 31)," +
                           " 'd' means month (1 - 12)," +
                           " 'e' means day of week (0 - 6) (Sunday=0)," +
                           " \"*\" means all")
        public String interval = "0,0,*,*,*";

        @Parameter(names = {"--backup-num"}, arity = 1,
                   validateWith = {UrlValidator.class},
                   description = "The number of latest backups to keep")
        public int num = 3;

        @Parameter(names = {"--directory", "-d"}, arity = 1,
                   validateWith = {UrlValidator.class}, required = true,
                   description = "The directory of backups stored")
        public String directory;
    }

    @Parameters(commandDescription = "Backup graph schema/data to files")
    public class Backup extends BackupRestore {

        @Parameter(names = {"--directory", "-d"}, arity = 1,
                   description = "Directory to store graph schema/data")
        public String directory = "./";

        public String directory() {
            return this.directory;
        }
    }

    @Parameters(commandDescription = "Restore graph schema/data from files")
    public class Restore extends BackupRestore {

        @ParametersDelegate
        private ExistDirectory directory = new ExistDirectory();

        public String directory() {
            return this.directory.directory;
        }
    }

    @Parameters(commandDescription = "Dump graph to files")
    public class DumpGraph {

        @ParametersDelegate
        private Retry retry = new Retry();

        @Parameter(names = {"--directory", "-d"}, arity = 1,
                   description = "Directory to store graph data")
        public String directory = "./";

        @Parameter(names = {"--formatter", "-f"}, arity = 1,
                   description = "Formatter to customize format of vertex/edge")
        public String formatter = "JsonFormatter";

        public int retry() {
            return this.retry.retry;
        }

        public String directory() {
            return this.directory;
        }

        public String formatter() {
            return this.formatter;
        }
    }

    @Parameters(commandDescription = "List all graphs")
    public class GraphList {
    }

    @Parameters(commandDescription = "Get graph info")
    public class GraphGet {

        @ParametersDelegate
        private GraphName graph = new GraphName();

        public String graph() {
            return this.graph.graphName;
        }
    }

    @Parameters(commandDescription = "Clear graph schema and data")
    public class GraphClear {

        @ParametersDelegate
        private GraphName graph = new GraphName();

        @ParametersDelegate
        private ConfirmMessage message = new ConfirmMessage();

        public String graph() {
            return this.graph.graphName;
        }

        public String confirmMessage() {
            return this.message.confirmMessage;
        }
    }

    @Parameters(commandDescription = "Set graph mode")
    public class GraphModeSet {

        @ParametersDelegate
        private GraphName graph = new GraphName();

        @ParametersDelegate
        private Mode mode = new Mode();

        public String graph() {
            return this.graph.graphName;
        }

        public GraphMode mode() {
            return this.mode.mode;
        }
    }

    @Parameters(commandDescription = "Get graph mode")
    public class GraphModeGet {

        @ParametersDelegate
        private GraphName graph = new GraphName();

        public String graph() {
            return this.graph.graphName;
        }
    }

    @Parameters(commandDescription = "Execute Gremlin statements")
    public class Gremlin extends GremlinJob {

        @ParametersDelegate
        private Aliases aliases = new Aliases();

        public Map<String, String> aliases() {
            return this.aliases.aliases;
        }
    }

    @Parameters(commandDescription = "Execute Gremlin statements as " +
                                     "asynchronous job")
    public class GremlinJob {

        @ParametersDelegate
        private FileScript fileScript = new FileScript();

        @ParametersDelegate
        private GremlinScript cmdScript = new GremlinScript();

        @ParametersDelegate
        private Language language = new Language();

        @ParametersDelegate
        private Bindings bindings = new Bindings();

        public String fileScript() {
            return this.fileScript.script;
        }

        public String cmdScript() {
            return this.cmdScript.script;
        }

        public String language() {
            return this.language.language;
        }

        public Map<String, String> bindings() {
            return this.bindings.bindings;
        }

        public String script() {
            String script;
            if (this.cmdScript() == null) {
                E.checkArgument(this.fileScript() != null,
                                "Either --script or --file must " +
                                "be provided, but both are null");
                script = this.fileScript();
            } else {
                E.checkArgument(this.fileScript() == null,
                                "Either --script or --file must " +
                                "be provided, but both are provided: " +
                                "'%s' and '%s'",
                                this.cmdScript(), this.fileScript());
                script = this.cmdScript();
            }
            return script;
        }
    }

    @Parameters(commandDescription = "List tasks")
    public class TaskList {

        @ParametersDelegate
        private TaskStatus status = new TaskStatus();

        @ParametersDelegate
        private Limit limit = new Limit();

        public String status() {
            return this.status.status.toUpperCase();
        }

        public long limit() {
            return this.limit.limit;
        }
    }

    @Parameters(commandDescription = "Get task info")
    public class TaskGet {

        @ParametersDelegate
        private TaskId taskId = new TaskId();

        public long taskId() {
            return this.taskId.taskId;
        }
    }

    @Parameters(commandDescription = "Delete task")
    public class TaskDelete {

        @ParametersDelegate
        private TaskId taskId = new TaskId();

        public long taskId() {
            return this.taskId.taskId;
        }
    }

    @Parameters(commandDescription = "Cancel task")
    public class TaskCancel {

        @ParametersDelegate
        private TaskId taskId = new TaskId();

        public long taskId() {
            return this.taskId.taskId;
        }
    }

    @Parameters(commandDescription = "Clear completed tasks")
    public class TaskClear {

        @Parameter(names = "--force",
                   description = "Force to clear all tasks, " +
                                 "cancel all uncompleted tasks firstly, " +
                                 "and delete all completed task")
        private boolean force = false;

        public boolean force() {
            return this.force;
        }
    }

    @Parameters(commandDescription = "Install HugeGraph-Server and " +
                                     "HugeGraph-Studio")
    public class Deploy {

        @ParametersDelegate
        private Version version = new Version();

        @ParametersDelegate
        public InstallPath path = new InstallPath();

        @ParametersDelegate
        public DownloadURL url = new DownloadURL();
    }

    @Parameters(commandDescription = "Start HugeGraph-Server and " +
                                     "HugeGraph-Studio")
    public class StartAll {

        @ParametersDelegate
        private Version version = new Version();

        @ParametersDelegate
        public InstallPath path = new InstallPath();
    }

    @Parameters(commandDescription = "Clear HugeGraph-Server and " +
                                     "HugeGraph-Studio")
    public class Clear {

        @ParametersDelegate
        public InstallPath path = new InstallPath();
    }

    @Parameters(commandDescription = "Stop HugeGraph-Server and " +
                                     "HugeGraph-Studio")
    public class StopAll {
    }

    @Parameters(commandDescription = "Print usage")
    public class Help {
    }

    public class BackupRestore {

        @ParametersDelegate
        private HugeTypes types = new HugeTypes();

        @ParametersDelegate
        private Retry retry = new Retry();

        public List<HugeType> types() {
            return this.types.types;
        }

        public int retry() {
            return this.retry.retry;
        }
    }

    public static class Url {

        @Parameter(names = {"--url"}, arity = 1,
                   validateWith = {UrlValidator.class},
                   description = "The URL of HugeGraph-Server url")
        public String url = "http://127.0.0.1:8080";
    }

    public static class Graph {

        @Parameter(names = {"--graph"}, arity = 1,
                   description = "Name of graph")
        public String graph = "hugegraph";
    }

    public static class Username {

        @Parameter(names = {"--user"}, arity = 1,
                   description = "User Name")
        public String username;
    }

    public static class Password {

        @Parameter(names = {"--password"}, arity = 1,
                   description = "Password of user")
        public String password;
    }

    public static class GraphName {

        @Parameter(names = {"--graph-name"}, arity = 1,
                   description = "Name of graph")
        public String graphName = "hugegraph";
    }

    public class HugeTypes {

        @Parameter(names = {"--huge-types", "-t"},
                   listConverter = HugeTypeListConverter.class,
                   required = true,
                   description = "Type of schema/data. " +
                                 "Concat with ',' if more than one. " +
                                 "'all' means 'vertex,edge,vertex_label," +
                                 "edge_label,property_key,index_label'")
        public List<HugeType> types;
    }

    public class ExistDirectory {

        @Parameter(names = {"--directory", "-d"}, arity = 1,
                   validateWith = {DirectoryValidator.class},
                   description = "Directory of graph schema/data")
        public String directory = "./";
    }

    public class InstallPath {

        @Parameter(names = {"-p"}, arity = 1, required = true,
                   description = "Install path of HugeGraph-Server and " +
                                 "HugeGraph-Studio")
        public String directory = null;
    }

    public class DownloadURL {

        @Parameter(names = {"-u"}, arity = 1,
                   description = "Download url prefix path of " +
                                 "HugeGraph-Server and HugeGraph-Studio")
        public String url = null;
    }

    public class ConfirmMessage {

        @Parameter(names = {"--confirm-message", "-c"}, arity = 1,
                   description = "Confirm message of graph clear",
                   required = true)
        public String confirmMessage;
    }

    public class Mode {

        @Parameter(names = {"--graph-mode", "-m"}, arity = 1,
                   converter = GraphModeConverter.class,
                   description = "Graph mode, " +
                                 "include: [NONE, RESTORING, MERGING]",
                   required = true)
        public GraphMode mode;
    }

    public class FileScript {

        @Parameter(names = {"--file", "-f"}, arity = 1,
                   converter = FileNameToContentConverter.class,
                   description = "Gremlin Script file to be executed, UTF-8" +
                                 "encoded, exclusive to --script")
        public String script;
    }

    public class GremlinScript {

        @Parameter(names = {"--script", "-s"}, arity = 1,
                   description = "Gremlin script to be executed, " +
                                 "exclusive to --file")
        public String script;
    }

    public class Language {

        @Parameter(names = {"--language", "-l"}, arity = 1,
                   description = "Gremlin script language")
        public String language = "gremlin-groovy";
    }

    public class Bindings {

        @Parameter(names = {"--bindings", "-b"}, arity = 1,
                   converter = MapConverter.class,
                   description = "Gremlin bindings, valid format is: " +
                                 "'key1=value1,key2=value2...'")
        public Map<String, String> bindings = ImmutableMap.of();
    }

    public class Aliases {

        @Parameter(names = {"--aliases", "-a"}, arity = 1,
                   converter = MapConverter.class,
                   description = "Gremlin aliases, valid format is: " +
                                 "'key1=value1,key2=value2...'")
        public Map<String, String> aliases = ImmutableMap.of();
    }

    public class Version {

        @Parameter(names = {"-v"}, arity = 1, required = true,
                   description = "Version of HugeGraph-Server and " +
                                 "HugeGraph-Studio")
        public String version;
    }

    public class Retry {

        @Parameter(names = {"--retry"}, arity = 1,
                   validateWith = {PositiveValidator.class},
                   description = "Retry times, default is 3")
        public int retry = 3;
    }

    public class Limit {

        @Parameter(names = {"--limit"}, arity = 1,
                   validateWith = {PositiveValidator.class},
                   description = "Limit number, no limit if not provided")
        public long limit = -1;
    }

    public class TaskStatus {

        @Parameter(names = {"--status"}, arity = 1,
                   validateWith = TaskStatusValidator.class,
                   description = "Status of task")
        public String status = null;
    }

    public class TaskId {

        @Parameter(names = {"--task-id"}, arity = 1, required = true,
                   validateWith = {PositiveValidator.class},
                   description = "Task id")
        private long taskId;
    }

    public static class GraphModeConverter
                  implements IStringConverter<GraphMode> {

        @Override
        public GraphMode convert(String value) {
            E.checkArgument(value != null && !value.isEmpty(),
                            "GraphMode can't be null or empty");
            return GraphMode.valueOf(value);
        }
    }

    public static class HugeTypeListConverter
                  implements IStringConverter<List<HugeType>> {

        @Override
        public List<HugeType> convert(String value) {
            E.checkArgument(value != null && !value.isEmpty(),
                            "HugeType can't be null or empty");
            String[] types = value.split(",");
            if (types.length == 1 && types[0].equalsIgnoreCase("all")) {
                return ImmutableList.of(HugeType.PROPERTY_KEY,
                                        HugeType.VERTEX_LABEL,
                                        HugeType.EDGE_LABEL,
                                        HugeType.INDEX_LABEL,
                                        HugeType.VERTEX,
                                        HugeType.EDGE);
            }
            List<HugeType> hugeTypes = new ArrayList<>();
            for (String type : types) {
                try {
                    hugeTypes.add(HugeType.valueOf(type.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new ParameterException(String.format(
                              "Invalid --type '%s', valid value is 'all' or " +
                              "combination of 'vertex,edge,vertex_label," +
                              "edge_label,property_key,index_label'", type));
                }
            }
            return hugeTypes;
        }
    }

    public static class MapConverter
                  implements IStringConverter<Map<String, String>> {

        @Override
        public Map<String, String> convert(String value) {
            E.checkArgument(value != null && !value.isEmpty(),
                            "HugeType can't be null or empty");
            String[] equals = value.split(",");
            Map<String, String> result = new HashMap<>();
            for (String equal : equals) {
                String[] kv = equal.split("=");
                E.checkArgument(kv.length == 2,
                                "Map arguments format should be key=value, " +
                                "but got '%s'", equal);
                result.put(kv[0], kv[1]);
            }
            return result;
        }
    }

    public static class FileNameToContentConverter
           implements IStringConverter<String> {

        @Override
        public String convert(String value) {
            File file = FileUtils.getFile(value);
            if (!file.exists() || !file.isFile() || !file.canRead()) {
                throw new ParameterException(String.format(
                          "'%s' must be existed readable file", value));
            }
            String content;
            try {
                content = FileUtils.readFileToString(file, API.CHARSET);
            } catch (IOException e) {
                throw new ParameterException(String.format(
                          "Read file '%s' error", value), e);
            }
            return content;
        }
    }

    public static class TaskStatusValidator implements IParameterValidator {

        @Override
        public void validate(String name, String value) {
            if (!TasksManager.TASK_STATUSES.contains(value.toUpperCase())) {
                throw new ParameterException(String.format(
                          "Invalid --status '%s', valid value is %s",
                          value, TasksManager.TASK_STATUSES));
            }
        }
    }

    public static class UrlValidator implements IParameterValidator {

        @Override
        public void validate(String name, String value) {
            String regex = "^((https|http|ftp|rtsp|mms)?://)"
                    + "?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?"
                    + "(([0-9]{1,3}\\.){3}[0-9]{1,3}" // IP URL, like: 10.0.0.1
                    + "|" // Or domain name
                    + "([0-9a-z_!~*'()-]+\\.)*" // Third level, like: www.
                    + "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\." // Second level
                    + "[a-z]{2,6})" // First level, like: com or museum
                    + "(:[0-9]{1,4})?"; // Port, like: 8080
            if (!value.matches(regex)) {
                throw new ParameterException(String.format(
                          "Invalid value of argument '%s': '%s'", name, value));
            }
        }
    }

    public static class DirectoryValidator implements IParameterValidator {

        @Override
        public void validate(String name, String value) {
            File file = new File(value);
            if (!file.exists() || !file.isDirectory()) {
                throw new ParameterException(String.format(
                          "Invalid value of argument '%s': '%s'", name, value));
            }
        }
    }

    public static class PositiveValidator implements IParameterValidator {

        @Override
        public void validate(String name, String value) {
            int retry = Integer.parseInt(value);
            if (retry <= 0) {
                throw new ParameterException(
                          "Parameter " + name + " should be positive, " +
                          "but got " + value);
            }
        }
    }
}
