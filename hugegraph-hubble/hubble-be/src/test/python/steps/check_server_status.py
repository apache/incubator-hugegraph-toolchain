# -*- coding: UTF-8 -*-

#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements. See the NOTICE file distributed with this
#  work for additional information regarding copyright ownership. The ASF
#  licenses this file to You under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#  License for the specific language governing permissions and limitations
#  under the License.

import json
import requests
import sys
from assertpy import assert_that
from behave import *
from imp import reload

reload(sys)
use_step_matcher("re")


@when("scene:(?P<scene>.+) url:(?P<url>.+)")
def step_impl(context, scene, url):
    http_url = "http://" + url + "/actuator/health"
    context.response = requests.get(http_url)
    context.code = context.response.status_code
    context.json = context.response.json()


@then("code:(?P<expect_code>.+) response:(?P<expect_json>.+)")
def step_impl(context, expect_code, expect_json):
    actual_code = context.code
    actual_json = context.json

    expect_code = int(expect_code)
    expect_json = json.loads(expect_json)

    assert_that(actual_code).described_as(context.response) \
        .is_equal_to(expect_code)
    assert_that(actual_json).described_as(context.response) \
        .is_equal_to(expect_json)

