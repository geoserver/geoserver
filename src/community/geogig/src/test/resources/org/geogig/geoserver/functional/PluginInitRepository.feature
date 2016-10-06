# (c) 2016 Open Source Geospatial Foundation - all rights reserved
# This code is licensed under the GPL 2.0 license, available at the root
# application directory.

@PluginRepositoryInitialization
Feature: Plugin Initialize Repository
  Creating a repository on the server is done through the "/repos/{repository}/init" API (for XML responses) or
  through the "/repos/{repository}/init.json" API (for JSON responses)
  The API is invoked using the HTTP PUT method, with optional JSON/Form request parameters that indicate if the
  repository should be created with a Directory backend or a PostgreSQL database backend
  If a repository with the provided name already exists, then a 409 "Conflict" error code shall be returned
  If the command succeeds, the response status code is 201 "Created"

  Scenario: Verify JSON formatted response
    Given There is an empty multirepo server
    When I call "PUT /repos/repo1/init.json"
    Then the response status should be '201'
    And the response ContentType should be "application/json"
    And the json object "response.success" equals "true"
    And the json object "response.repo.name" equals "repo1"
    And the json object "response.repo.href" ends with "/repos/repo1.json"
    And the parent directory of repository "repo1" is NOT the System Temp directory

  Scenario: Verify JSON fomratted response of Init with JSON formatted request parameters
    Given There is an empty multirepo server
    When I call "PUT /repos/repo1/init.json" with the System Temp Directory as the parentDirectory
    Then the response status should be '201'
    And the response ContentType should be "application/json"
    And the json object "response.success" equals "true"
    And the json object "response.repo.name" equals "repo1"
    And the json object "response.repo.href" ends with "/repos/repo1.json"
    And the parent directory of repository "repo1" equals System Temp directory

  Scenario: Verify XML fomratted response of Init with JSON formatted request parameters
    Given There is an empty multirepo server
    When I call "PUT /repos/repo1/init" with the System Temp Directory as the parentDirectory
    Then the response status should be '201'
    And the response ContentType should be "application/xml"
    And the xpath "/response/success/text()" equals "true"
    And the xpath "/response/repo/name/text()" equals "repo1"
    And the xpath "/response/repo/atom:link/@href" contains "/repos/repo1.xml"
    And the parent directory of repository "repo1" equals System Temp directory

  Scenario: Verify JSON fomratted response of Init with URL Form encoded request parameters
    Given There is an empty multirepo server
    When I call "PUT /repos/repo1/init.json" with a URL encoded Form containing a parentDirectory parameter
    Then the response status should be '201'
    And the response ContentType should be "application/json"
    And the json object "response.success" equals "true"
    And the json object "response.repo.name" equals "repo1"
    And the json object "response.repo.href" ends with "/repos/repo1.json"
    And the parent directory of repository "repo1" equals System Temp directory

  Scenario: Verify XML fomratted response of Init with URL Form encoded request parameters
    Given There is an empty multirepo server
    When I call "PUT /repos/repo1/init" with a URL encoded Form containing a parentDirectory parameter
    Then the response status should be '201'
    And the response ContentType should be "application/xml"
    And the xpath "/response/success/text()" equals "true"
    And the xpath "/response/repo/name/text()" equals "repo1"
    And the xpath "/response/repo/atom:link/@href" contains "/repos/repo1.xml"
    And the parent directory of repository "repo1" equals System Temp directory

  Scenario: Verify JSON fomratted response of Init with already existing repository
    Given There is a default multirepo server
    When I call "PUT /repos/repo1/init.json" with the System Temp Directory as the parentDirectory
    Then the response status should be '409'
    And the response ContentType should be "application/json"
    And the json object "response.success" equals "false"
    And the json object "response.error" equals "Cannot run init on an already initialized repository."

  Scenario: Verify JSON fomratted response of Init with JSON formatted request parameters and Author
    Given There is an empty multirepo server
    When I call "PUT /repos/repo1/init.json" with Author and the System Temp Directory as the parentDirectory
    Then the response status should be '201'
    And the response ContentType should be "application/json"
    And the json object "response.success" equals "true"
    And the json object "response.repo.name" equals "repo1"
    And the json object "response.repo.href" ends with "/repos/repo1.json"
    And the parent directory of repository "repo1" equals System Temp directory
    And the Author config of repository "repo1" is set

  Scenario: Verify XML fomratted response of Init with JSON formatted request parameters and Author
    Given There is an empty multirepo server
    When I call "PUT /repos/repo1/init" with Author and the System Temp Directory as the parentDirectory
    Then the response status should be '201'
    And the response ContentType should be "application/xml"
    And the xpath "/response/success/text()" equals "true"
    And the xpath "/response/repo/name/text()" equals "repo1"
    And the xpath "/response/repo/atom:link/@href" contains "/repos/repo1.xml"
    And the parent directory of repository "repo1" equals System Temp directory
    And the Author config of repository "repo1" is set

  Scenario: Verify JSON fomratted response of Init with URL Form encoded request parameters and Author
    Given There is an empty multirepo server
    When I call "PUT /repos/repo1/init.json" with a URL encoded Form containing a parentDirectory parameter and Author
    Then the response status should be '201'
    And the response ContentType should be "application/json"
    And the json object "response.success" equals "true"
    And the json object "response.repo.name" equals "repo1"
    And the json object "response.repo.href" ends with "/repos/repo1.json"
    And the parent directory of repository "repo1" equals System Temp directory
    And the Author config of repository "repo1" is set

  Scenario: Verify XML fomratted response of Init with URL Form encoded request parameters and Author
    Given There is an empty multirepo server
    When I call "PUT /repos/repo1/init" with a URL encoded Form containing a parentDirectory parameter and Author
    Then the response status should be '201'
    And the response ContentType should be "application/xml"
    And the xpath "/response/success/text()" equals "true"
    And the xpath "/response/repo/name/text()" equals "repo1"
    And the xpath "/response/repo/atom:link/@href" contains "/repos/repo1.xml"
    And the parent directory of repository "repo1" equals System Temp directory
    And the Author config of repository "repo1" is set

  Scenario: Verify Init with unsupported MediaType does not create a repository with defualt settings
    Given There is an empty multirepo server
    When I call "PUT /repos/repo1/init.json" with an unsupported media type
    Then the response status should be '400'
    And there should be no "repo1" created
