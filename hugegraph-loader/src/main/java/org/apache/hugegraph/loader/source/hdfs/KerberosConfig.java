/*
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

package org.apache.hugegraph.loader.source.hdfs;

import java.io.File;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import org.apache.hugegraph.loader.constant.Checkable;
import org.apache.hugegraph.util.E;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KerberosConfig implements Checkable {

    @JsonProperty("enable")
    private boolean enable;
    @JsonProperty("krb5_conf")
    private String krb5Conf;
    @JsonProperty("principal")
    private String principal;
    @JsonProperty("keytab")
    private String keyTab;

    @Override
    public void check() throws IllegalArgumentException {
        if (this.enable) {
            E.checkArgument(!StringUtils.isEmpty(this.krb5Conf),
                            "The krb5_conf can't be empty");
            File krb5ConfFile = FileUtils.getFile(Paths.get(this.krb5Conf)
                                                       .toString());
            E.checkArgument(krb5ConfFile.exists() && krb5ConfFile.isFile(),
                            "The krb5 conf file '%s' is not an existing file",
                            krb5ConfFile);

            E.checkArgument(!StringUtils.isEmpty(this.principal),
                            "The principal can't be empty");

            E.checkArgument(!StringUtils.isEmpty(this.keyTab),
                            "The keytab can't be empty");
            File keyTabFile = FileUtils.getFile(Paths.get(this.keyTab)
                                                     .toString());
            E.checkArgument(keyTabFile.exists() && keyTabFile.isFile(),
                            "The key tab file '%s' is not an existing file",
                            keyTabFile);
        }
    }

    public boolean enable() {
        return this.enable;
    }

    public String krb5Conf() {
        return this.krb5Conf;
    }

    public String principal() {
        return this.principal;
    }

    public String keyTab() {
        return this.keyTab;
    }
}
