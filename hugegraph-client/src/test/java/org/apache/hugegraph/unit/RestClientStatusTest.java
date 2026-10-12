/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.unit;

import org.apache.hugegraph.client.RestClient;
import org.apache.hugegraph.exception.ServerException;
import org.junit.Test;
import org.mockito.Mockito;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;

public class RestClientStatusTest extends BaseUnitTest {

    @Test
    public void testOkIsAcceptedForDeleteSuccessStatuses() {
        okhttp3.Response response = Mockito.mock(okhttp3.Response.class);
        Request request = new Request.Builder().url("http://127.0.0.1:1")
                                               .delete()
                                               .build();
        Mockito.when(response.code()).thenReturn(200);
        Mockito.when(response.request()).thenReturn(request);

        new TestRestClient().check(response, 202, 204);
    }

    @Test(expected = ServerException.class)
    public void testOkIsRejectedForNonDeleteExpectedStatus() {
        Request request = new Request.Builder().url("http://127.0.0.1:1")
                                               .post(okhttp3.RequestBody.create(null,
                                                                               new byte[0]))
                                               .build();
        okhttp3.Response response = new okhttp3.Response.Builder()
                                    .request(request)
                                    .protocol(Protocol.HTTP_1_1)
                                    .code(200)
                                    .message("OK")
                                    .body(ResponseBody.create(
                                          MediaType.parse("application/json"), "{}"))
                                    .build();

        new TestRestClient().check(response, 201, 202);
    }

    private static class TestRestClient extends RestClient {

        TestRestClient() {
            super("http://127.0.0.1:1", "", "", 1);
        }

        void check(okhttp3.Response response, int... statuses) {
            this.checkStatus(response, statuses);
        }
    }
}
