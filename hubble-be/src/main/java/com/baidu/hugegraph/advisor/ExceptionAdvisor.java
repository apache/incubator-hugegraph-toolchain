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

package com.baidu.hugegraph.advisor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.baidu.hugegraph.common.Response;
import com.baidu.hugegraph.exception.ExternalException;
import com.baidu.hugegraph.exception.InternalException;
import com.baidu.hugegraph.exception.ParameterizedException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class ExceptionAdvisor {

    @Autowired
    private MessageSourceHandler messageSourceHandler;

    @ExceptionHandler(InternalException.class)
    @ResponseStatus(HttpStatus.OK)
    public Response exceptionHandler(InternalException e) {
        String message = this.handleMessage(e.getMessage(), e.args());
        return Response.builder()
                       .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                       .message(message)
                       .build();
    }

    @ExceptionHandler(ExternalException.class)
    @ResponseStatus(HttpStatus.OK)
    public Response exceptionHandler(ExternalException e) {
        String message = this.handleMessage(e.getMessage(), e.args());
        return Response.builder()
                       .status(HttpStatus.BAD_REQUEST.value())
                       .message(message)
                       .build();
    }

    @ExceptionHandler(ParameterizedException.class)
    @ResponseStatus(HttpStatus.OK)
    public Response exceptionHandler(ParameterizedException e) {
        String message = this.handleMessage(e.getMessage(), e.args());
        return Response.builder()
                       .status(HttpStatus.BAD_REQUEST.value())
                       .message(message)
                       .build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Response exceptionHandler(Exception e) {
        String message = this.handleMessage(e.getMessage(), null);
        return Response.builder()
                       .status(HttpStatus.BAD_REQUEST.value())
                       .message(message)
                       .build();
    }

    private String handleMessage(String message, Object[] args) {
        String[] strArgs = null;
        if (args != null && args.length > 0) {
            strArgs = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    strArgs[i] = args[i].toString();
                } else {
                    strArgs[i] = "?";
                }
            }
        }
        try {
            message = this.messageSourceHandler.getMessage(message, strArgs);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
        return message;
    }
}
