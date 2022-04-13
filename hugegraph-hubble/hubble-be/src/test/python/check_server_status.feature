# language: en
Feature: check hugegraph-hubble server health status

  Scenario Outline: check hugegraph-hubble server health status
    When  scene:<scene> url:<url>
    Then  code:<code> response:<response>
    Examples:
      | scene                      | url            | code | response        |
      | check server health status | localhost:8088 | 200  | {"status":"UP"} |
