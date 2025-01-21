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

package org.apache.hugegraph.api.auth;

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.api.API;
import org.apache.hugegraph.rest.RestResult;

import org.apache.hugegraph.structure.auth.LoginResult;
import org.apache.hugegraph.structure.constant.HugeType;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SaasLoginAPI extends API {

    private static final String PATH = "saas/auth/login";

    public SaasLoginAPI(RestClient client) {
        super(client);
    }

    @Override
    protected String type() {
        return HugeType.LOGIN.string();
    }

    public LoginResult saasLogin(SaasLogin login) {
        RestResult result = this.client.post(PATH, login);
        return result.readObject(LoginResult.class);
    }

    public static class SaasLogin {
        @JsonProperty("user_name")
        private String name;

        @JsonProperty("user_password")
        private String password;

        @JsonProperty("user_nickname")
        private String nickname;

        @JsonProperty("user_email")
        private String email;

        @JsonProperty("token_expire")
        private long expire;

        @JsonProperty("token")
        private String token;

        public void name(String name) {
            this.name = name;
        }

        public String name() {
            return this.name;
        }

        public void password(String password) {
            this.password = password;
        }

        public String password() {
            return this.password;
        }

        public void expire(long expire) {
            this.expire = expire;
        }

        public long expire() {
            return this.expire;
        }

        public void token(String token) {
            this.token = token;
        }

        public String token() {
            return this.token;
        }

        public void email(String email) {
            this.email = email;
        }

        public String email() {
            return this.email;
        }

        public void nickname(String nickname) {
            this.nickname = nickname;
        }

        public String nickname() {
            return this.nickname;
        }
    }
}
