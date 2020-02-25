# (c) 2017 Open Source Geospatial Foundation - all rights reserved
# This code is licensed under the GPL 2.0 license, available at the root
# application directory.

Feature: GeoGig Repository initialization tests specific to the GeoServer Plugin

  @CreateRepository @Plugin
  Scenario: Verify trying to create a repo issues 409 "Conflict" when a repo with the same name already exists
    Given There is a default multirepo server
    When I "PUT" content-type "application/json" to "/repos/repo1/init.json" with
      """
      {
      }
      """
    Then the response status should be '409'
    And the response ContentType should be "application/json"
    And the json object "response.success" equals "false"
    And the json object "response.error" equals "The specified repository name is already in use, please try a different name"

  @CreateRepository @Plugin
  Scenario: Verify trying to create a repo issues 409 "Conflict" when a repo with the same name already exists, with parentDirectory
    Given There is a default multirepo server
    When I "PUT" content-type "application/json" to "/repos/repo1/init.json" with
      """
      {
        "parentDirectory": "{@systemTempPath}"
      }
      """
    Then the response status should be '409'
    And the response ContentType should be "application/json"
    And the json object "response.success" equals "false"
    And the json object "response.error" equals "The specified repository name is already in use, please try a different name"

  @CreateRepository @Plugin
  Scenario: Verify trying to create a repo issues 409 "Conflict" when a repo with the same name already exists, with arbitrary parentDirectory
    Given There is a default multirepo server
    When I "PUT" content-type "application/json" to "/repos/repo1/init.json" with
      """
      {
        "parentDirectory": "C:\\tmp"
      }
      """
    Then the response status should be '409'
    And the response ContentType should be "application/json"
    And the json object "response.success" equals "false"
    And the json object "response.error" equals "The specified repository name is already in use, please try a different name"
