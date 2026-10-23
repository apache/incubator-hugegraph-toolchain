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

package org.apache.hugegraph.controller.op;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.hugegraph.service.op.LogService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletResponse;

public class LogControllerTest {

    @Test
    public void testExportFilenameUsesBusinessTimezoneWhenJvmUsesUtc() throws Exception {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            LogController controller = new LogController();
            controller.logService = Mockito.mock(LogService.class);
            LogService.LogReq request = new LogService.LogReq();
            request.startDatetime = Date.from(Instant.parse("2026-07-12T16:00:00Z"));
            request.endDatetime = Date.from(Instant.parse("2026-07-12T17:02:03Z"));
            Mockito.when(controller.logService.export(request)).thenReturn(List.of());
            MockHttpServletResponse response = new MockHttpServletResponse();

            controller.export(response, request);

            Assert.assertEquals("attachment;filename=log_20260713000000_20260713010203.txt",
                                response.getHeader("Content-Disposition"));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    public void testExportFilenameRetainsOpenEndedRange() throws Exception {
        LogController controller = new LogController();
        controller.logService = Mockito.mock(LogService.class);
        LogService.LogReq request = new LogService.LogReq();
        Mockito.when(controller.logService.export(request)).thenReturn(List.of());
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.export(response, request);

        Assert.assertEquals("attachment;filename=log_begin_end.txt", response.getHeader("Content-Disposition"));
    }
}
