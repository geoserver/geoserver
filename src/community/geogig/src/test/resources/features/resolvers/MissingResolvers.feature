# (c) 2017 Open Source Geospatial Foundation - all rights reserved
# This code is licensed under the GPL 2.0 license, available at the root
# application directory.

@ImportExisting @Plugin @MissingBackend
Feature: Tests Import Existing repo behavior when certain repository backends backend are not available

  Scenario: Import of existing RocksDB repository fails with missing RocksDB resolver
    Given There is an empty multirepo server
    And I have "geogigRepo" that is not managed
    And I have disabled backends: "Directory"
    When I "POST" content-type "application/json" to "/repos/geogigRepo/importExistingRepo" with
      """
      {
        "parentDirectory":"{@systemTempPath}",
        "leafDirectory":"nonExistentRepo",
        "authorName":"GeoGig User",
        "authorEmail":"geogig@geogig.org"
      }
      """
    Then the response status should be '400'
    And the response ContentType should be "application/xml"
    And the xpath "/response/success/text()" equals "false"
    And the xpath "/response/error/text()" contains "No repository initializer found capable of handling this kind of URI: file:/"

  Scenario: Import of existing RocksDB repository fails with missing RocksDB resolver, JSON response
    Given There is an empty multirepo server
    And I have "geogigRepo" that is not managed
    And I have disabled backends: "Directory"
    When I "POST" content-type "application/json" to "/repos/geogigRepo/importExistingRepo.json" with
      """
      {
        "parentDirectory":"{@systemTempPath}",
        "leafDirectory":"nonExistentRepo",
        "authorName":"GeoGig User",
        "authorEmail":"geogig@geogig.org"
      }
      """
    Then the response status should be '400'
    And the response ContentType should be "application/json"
    And the json object "response.success" equals "false"
    And the json response "response.error" should contain "No repository initializer found capable of handling this kind of URI: file:/"

  Scenario: Import of existing RocksDB repository fails with missing RocksDB and PostgreSQL resolvers
    Given There is an empty multirepo server
    And I have "geogigRepo" that is not managed
    And I have disabled backends: "Directory, PostgreSQL"
    When I "POST" content-type "application/json" to "/repos/geogigRepo/importExistingRepo" with
      """
      {
        "parentDirectory":"{@systemTempPath}",
        "leafDirectory":"nonExistentRepo",
        "authorName":"GeoGig User",
        "authorEmail":"geogig@geogig.org"
      }
      """
    Then the response status should be '400'
    And the response ContentType should be "application/xml"
    And the xpath "/response/success/text()" equals "false"
    And the xpath "/response/error/text()" contains "No repository initializer found capable of handling this kind of URI: file:/"

  Scenario: Import of existing RocksDB repository fails with missing RocksDB and PostgreSQL resolvers, JSON response
    Given There is an empty multirepo server
    And I have "geogigRepo" that is not managed
    And I have disabled backends: "Directory, PostgreSQL"
    When I "POST" content-type "application/json" to "/repos/geogigRepo/importExistingRepo.json" with
      """
      {
        "parentDirectory":"{@systemTempPath}",
        "leafDirectory":"nonExistentRepo",
        "authorName":"GeoGig User",
        "authorEmail":"geogig@geogig.org"
      }
      """
    Then the response status should be '400'
    And the response ContentType should be "application/json"
    And the json object "response.success" equals "false"
    And the json response "response.error" should contain "No repository initializer found capable of handling this kind of URI: file:/"

  Scenario: Import of PostgreSQL repository fails with missing PostgreSQL resolver
    Given There is an empty multirepo server
    And I have disabled backends: "PostgreSQL"
    When I "POST" content-type "application/json" to "/repos/geogigRepo/importExistingRepo" with
      """
      {
        "dbName":"database",
        "dbPassword":"password"
      }
      """
    Then the response status should be '400'
    And the response ContentType should be "application/xml"
    And the xpath "/response/success/text()" equals "false"
    And the xpath "/response/error/text()" contains "No repository initializer found capable of handling this kind of URI: postgresql:/"

  Scenario: Import of PostgreSQL repository fails with missing PostgreSQL resolver, JSON response
    Given There is an empty multirepo server
    And I have disabled backends: "PostgreSQL"
    When I "POST" content-type "application/json" to "/repos/geogigRepo/importExistingRepo.json" with
      """
      {
        "dbName":"database",
        "dbPassword":"password"
      }
      """
    Then the response status should be '400'
    And the response ContentType should be "application/json"
    And the json object "response.success" equals "false"
    And the json response "response.error" should contain "No repository initializer found capable of handling this kind of URI: postgresql:/"

  Scenario: Import of existing RocksDB repository passes with missing PostgreSQL resolver
    Given There is an empty multirepo server
    And I have "geogigRepo" that is not managed
    And I have disabled backends: "PostgreSQL"
    When I "POST" content-type "application/json" to "/repos/geogigRepo/importExistingRepo" with
      """
      {
        "parentDirectory":"{@systemTempPath}",
        "leafDirectory":"geogigRepo"
      }
      """
    Then the response status should be '200'
    And the response ContentType should be "application/xml"
    And the xpath "/response/success/text()" equals "true"
    And the xpath "/response/repo/name/text()" equals "geogigRepo"
    And the xpath "/response/repo/atom:link/@href" contains "/repos/geogigRepo.xml"

  Scenario: Import of existing RocksDB repository passes with missing PostgreSQL resolver, JSON response
    Given There is an empty multirepo server
    And I have "geogigRepo" that is not managed
    And I have disabled backends: "PostgreSQL"
    When I "POST" content-type "application/json" to "/repos/geogigRepo/importExistingRepo.json" with
      """
      {
        "parentDirectory":"{@systemTempPath}",
        "leafDirectory":"geogigRepo"
      }
      """
    Then the response status should be '200'
    And the response ContentType should be "application/json"
    And the json object "response.success" equals "true"
    And the json object "response.repo.name" equals "geogigRepo"
    And the json object "response.repo.href" ends with "/repos/geogigRepo.json"
