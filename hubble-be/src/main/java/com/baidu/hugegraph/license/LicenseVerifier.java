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

package com.baidu.hugegraph.license;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.prefs.Preferences;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import com.baidu.hugegraph.HugeGraphHubble;
import com.baidu.hugegraph.common.Constant;
import com.baidu.hugegraph.exception.ExternalException;
import com.baidu.hugegraph.exception.InternalException;
import com.baidu.hugegraph.util.HubbleUtil;
import com.baidu.hugegraph.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.schlichtherle.license.CipherParam;
import de.schlichtherle.license.DefaultCipherParam;
import de.schlichtherle.license.DefaultKeyStoreParam;
import de.schlichtherle.license.DefaultLicenseParam;
import de.schlichtherle.license.KeyStoreParam;
import de.schlichtherle.license.LicenseContent;
import de.schlichtherle.license.LicenseParam;

public class LicenseVerifier {

    private static final Logger LOG = Log.logger(HugeGraphHubble.class);

    private static final String LICENSE_PARAM_PATH = "/verify-license.json";

    private static volatile LicenseVerifier INSTANCE = null;

    private static final Duration CHECK_INTERVAL = Duration.ofMinutes(10);
    private volatile Instant lastCheckTime = Instant.now();

    private final LicenseVerifyParam verifyParam;
    private final LicenseVerifyManager manager;

    private final String edition;

    private LicenseVerifier() {
        this.verifyParam = buildVerifyParam(LICENSE_PARAM_PATH);
        // TODO: replaced by reading from ExtraParam
        String licensePath = this.verifyParam.licensePath();
        if (licensePath.contains("community")) {
            this.edition = Constant.EDITION_COMMUNITY;
        } else {
            this.edition = Constant.EDITION_COMMERCIAL;
        }
        LicenseParam licenseParam = this.initLicenseParam(this.verifyParam);
        this.manager = new LicenseVerifyManager(licenseParam);
    }

    public static LicenseVerifier instance() {
        if (INSTANCE == null) {
            synchronized(LicenseVerifier.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LicenseVerifier();
                }
            }
        }
        return INSTANCE;
    }

    public String edition() {
        return this.edition;
    }

    public int allowedGraphs() {
        return this.manager.allowedGraphs();
    }

    public long allowedDataSize() {
        return this.manager.allowedDataSize();
    }

    public void verifyIfNeeded() {
        Instant now = HubbleUtil.nowTime();
        Duration interval = Duration.between(this.lastCheckTime, now);
        if (!interval.minus(CHECK_INTERVAL).isNegative()) {
            this.verify();
            this.lastCheckTime = now;
        }
    }

    public synchronized void install(ServerInfo serverInfo, String md5) {
        this.manager.serverInfo(serverInfo);
        try {
            this.manager.uninstall();
            File licenseFile = new File(this.verifyParam.licensePath());
            this.verifyPublicCert(md5);
            LicenseContent content = this.manager.install(licenseFile);
            LOG.info("The license is successfully installed, valid for {} - {}",
                     content.getNotBefore(), content.getNotAfter());
        } catch (ExternalException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalException("license.install.failed", e);
        }
    }

    public void verify() {
        try {
            LicenseContent content = this.manager.verify();
            LOG.info("The license verification passed, valid for {} - {}",
                     content.getNotBefore(), content.getNotAfter());
        } catch (ExternalException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalException("license.verify.failed", e);
        }
    }

    private void verifyPublicCert(String expectMD5) {
        String path = this.verifyParam.publicKeyPath();
        try (InputStream is = LicenseVerifier.class.getResourceAsStream(path)) {
            String actualMD5 = DigestUtils.md5Hex(is);
            if (!actualMD5.equals(expectMD5)) {
                throw new ExternalException("license.public-cert.invalid");
            }
        } catch (IOException e) {
            throw new InternalException("license.read.public-cert.failed", e);
        }
    }

    private LicenseParam initLicenseParam(LicenseVerifyParam param) {
        Preferences preferences = Preferences.userNodeForPackage(
                                  LicenseVerifier.class);
        CipherParam cipherParam = new DefaultCipherParam(
                                  param.storePassword());
        KeyStoreParam keyStoreParam = new DefaultKeyStoreParam(
                                      LicenseVerifier.class,
                                      param.publicKeyPath(),
                                      param.publicAlias(),
                                      param.storePassword(),
                                      null);
        return new DefaultLicenseParam(param.subject(), preferences,
                                       keyStoreParam, cipherParam);
    }

    private static LicenseVerifyParam buildVerifyParam(String path) {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream stream =
             LicenseVerifier.class.getResourceAsStream(path)) {
            return mapper.readValue(stream, LicenseVerifyParam.class);
        } catch (IOException e) {
            throw new InternalException("license.read.failed", e);
        }
    }
}
