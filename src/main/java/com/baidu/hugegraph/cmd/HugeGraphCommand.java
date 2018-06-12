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

import java.util.Iterator;
import java.util.Map;

import com.baidu.hugegraph.cmd.manager.BackupManager;
import com.baidu.hugegraph.cmd.manager.GraphsManager;
import com.baidu.hugegraph.cmd.manager.GremlinManager;
import com.baidu.hugegraph.cmd.manager.RestoreManager;
import com.baidu.hugegraph.cmd.manager.ToolManager;
import com.baidu.hugegraph.structure.gremlin.Result;
import com.baidu.hugegraph.structure.gremlin.ResultSet;
import com.baidu.hugegraph.util.E;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;

public class HugeGraphCommand {

    private SubCommands subCommands;

    @ParametersDelegate
    private SubCommands.Url url = new SubCommands.Url();

    @ParametersDelegate
    private SubCommands.Graph graph = new SubCommands.Graph();

    @ParametersDelegate
    private SubCommands.Username username = new SubCommands.Username();

    @ParametersDelegate
    private SubCommands.Password password = new SubCommands.Password();

    public HugeGraphCommand() {
        this.subCommands = new SubCommands();
    }

    public Map<String, Object> subCommands() {
        return this.subCommands.commands();
    }

    public String url() {
        return this.url.url;
    }

    public String graph() {
        return this.graph.graph;
    }

    public String username() {
        return this.username.username;
    }

    public String password() {
        return this.password.password;
    }

    public JCommander jCommander() {
        JCommander.Builder builder = JCommander.newBuilder();

        // Add main command firstly
        builder.addObject(this);

        // Add sub-commands
        for (Map.Entry<String, Object> entry : this.subCommands().entrySet()) {
            builder.addCommand(entry.getKey(), entry.getValue());
        }

        JCommander jCommander = builder.build();
        jCommander.setProgramName("hugegraph");
        jCommander.setCaseSensitiveOptions(true);
        jCommander.setAllowAbbreviatedOptions(true);
        return jCommander;
    }

    private void execute(String subCommand, JCommander jCommander) {
        this.checkMainParams();
        switch (subCommand) {
            case "backup":
                Printer.print("Graph '%s' start back up!", this.graph());
                SubCommands.Backup backup =
                        (SubCommands.Backup) this.subCommands().get(subCommand);
                BackupManager backupManager = manager(BackupManager.class);
                backupManager.retry(backup.retry());
                backupManager.backup(backup.types(), backup.directory());
                break;
            case "restore":
                Printer.print("Graph '%s' start restore!", this.graph());
                SubCommands.Restore restore =
                        (SubCommands.Restore) this.subCommands()
                                                  .get(subCommand);
                RestoreManager restoreManager = manager(RestoreManager.class);
                restoreManager.retry(restore.retry());
                restoreManager.restore(restore.types(), restore.directory());
                break;
            case "graph-list":
                GraphsManager graphsManager = manager(GraphsManager.class);
                Printer.printList("Graphs", graphsManager.list());
                break;
            case "graph-get":
                SubCommands.GraphGet graphGet =
                        (SubCommands.GraphGet) this.subCommands()
                                                   .get(subCommand);
                graphsManager = manager(GraphsManager.class);
                Printer.printMap("Graph info",
                                 graphsManager.get(graphGet.graph()));
                break;
            case "graph-clear":
                SubCommands.GraphClear graphClear =
                        (SubCommands.GraphClear) this.subCommands()
                                                     .get(subCommand);
                graphsManager = manager(GraphsManager.class);
                graphsManager.clear(graphClear.graph(),
                                    graphClear.confirmMessage());
                Printer.print("Graph '%s' is cleared", graphClear.graph());
                break;
            case "graph-mode-set":
                SubCommands.GraphModeSet graphModeSet =
                        (SubCommands.GraphModeSet) this.subCommands()
                                                       .get(subCommand);
                graphsManager = manager(GraphsManager.class);
                graphsManager.restoring(graphModeSet.graph(),
                                        graphModeSet.restoreFlag());
                Printer.print("set graph '%s' restoring to '%s'",
                              graphModeSet.graph(), graphModeSet.restoreFlag());
                break;
            case "graph-mode-get":
                SubCommands.GraphModeGet graphModeGet =
                        (SubCommands.GraphModeGet) this.subCommands()
                                                       .get(subCommand);
                graphsManager = manager(GraphsManager.class);
                Printer.printKV("restoring",
                                graphsManager.restoring(graphModeGet.graph()));
                break;
            case "gremlin":
                SubCommands.Gremlin gremlin =
                        (SubCommands.Gremlin) this.subCommands()
                                                  .get(subCommand);
                GremlinManager gremlinManager = manager(GremlinManager.class);
                ResultSet result = gremlinManager.execute(gremlin.script(),
                                                          gremlin.bindings(),
                                                          gremlin.language(),
                                                          gremlin.aliases());
                Iterator<Result> iterator = result.iterator();
                while (iterator.hasNext()) {
                    Printer.print(iterator.next().getString());
                }
                break;
            case "help":
                jCommander.usage();
                break;
            default:
                throw new ParameterException(String.format(
                          "Invalid sub-command: %s", subCommand));
        }
    }

    private void checkMainParams() {
        E.checkArgument(this.url() != null, "Url can't be null");
        E.checkArgument(this.username() == null && this.password() == null ||
                        this.username() != null && this.password() != null,
                        "Both user name and password must be null or " +
                        "not null at same time");
    }

    @SuppressWarnings("unchecked")
    private <T extends ToolManager> T manager(Class<T> clz) {
        try {
            if (clz == GraphsManager.class) {
                if (this.username() != null) {
                    return (T) new GraphsManager(this.url(), this.username(),
                                                 this.password());
                } else {
                    return (T) new GraphsManager(this.url());
                }
            } else {
                if (this.username() != null) {
                    return clz.getConstructor(String.class, String.class,
                                              String.class, String.class)
                              .newInstance(this.url(), this.graph(),
                                           this.username(), this.password());
                } else {
                    return clz.getConstructor(String.class, String.class)
                              .newInstance(this.url(), this.graph());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format(
                      "Construct manager failed for class '%s'", clz), e);
        }
    }

    public static void main(String[] args) {
        HugeGraphCommand cmd = new HugeGraphCommand();
        JCommander jCommander = cmd.jCommander();

        if (args.length == 0) {
            jCommander.usage();
            System.exit(-1);
        }
        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            Printer.print(e.getMessage());
            jCommander.usage();
            System.exit(-1);
        }

        String subCommand = jCommander.getParsedCommand();
        if (subCommand == null) {
            Printer.print("Must provide one sub-command");
            jCommander.usage();
            System.exit(-1);
        }

        cmd.execute(subCommand, jCommander);
    }
}
