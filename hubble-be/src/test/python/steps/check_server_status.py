# -*- coding: UTF-8 -*-

import json
import sys

import requests
from assertpy import assert_that
from behave import *

reload(sys)
sys.setdefaultencoding('utf8')
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

