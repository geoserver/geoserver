# (c) 2017 Open Source Geospatial Foundation - all rights reserved
# This code is licensed under the GPL 2.0 license, available at the root
# application directory.

Feature: Import an Existing GeoGig repository
  The import of a repository is done by issuing a POST request
  to "/repos/<REPO_NAME>/importExistingRepo" API (for an XML response)
  and "/repos/<REPO_NAME>/importExistingRepo.json" API (for a JSON response).
  The response should be a 200 OK and contain the name of the new repository as well as
  an href URL.

  @ImportExisting @Plugin
  Scenario: Import of non existent repository fails
    Given There is a default multirepo server
    And I have "geogigRepo" that is not managed
    When I "POST" content-type "application/json" to "/repos/nonExistentRepo/importExistingRepo" with
      """
      {
        "parentDirectory":"{@systemTempPath}",
        "authorName":"GeoGig User",
        "authorEmail":"geogig@geogig.org"
      }
      """
    Then there should be no "nonExistentRepo" created
    And the response body should contain "Repository not found"

  @ImportExisting @Plugin
  Scenario: Import of existent repository succeeds with an XML response
    Given There is a default multirepo server
    And I have "geogigRepo" that is not managed
    When I "POST" content-type "application/json" to "/repos/geogigRepo/importExistingRepo" with
      """
      {
        "parentDirectory":"{@systemTempPath}",
        "authorName":"GeoGig User",
        "authorEmail":"geogig@geogig.org"
      }
      """
    Then the response status should be '200'
    And the response body should contain "http://localhost:8080/geoserver/geogig/repos/geogigRepo.xml"

  @ImportExisting @Plugin
  Scenario: Import of existent repository succeeds with a JSON response
    Given There is a default multirepo server
    And I have "geogigRepo" that is not managed
    When I "POST" content-type "application/json" to "/repos/geogigRepo/importExistingRepo.json" with
      """
      {
        "parentDirectory":"{@systemTempPath}",
        "authorName":"GeoGig User",
        "authorEmail":"geogig@geogig.org"
      }
      """
    Then the response status should be '200'
    And the response body should contain "http://localhost:8080/geoserver/geogig/repos/geogigRepo.json"

  @ImportExisting @Plugin
  Scenario: Import of a repository that is already managed fails with a 409 Conflict
    Given There is a default multirepo server
    When I "POST" content-type "application/json" to "/repos/repo1/importExistingRepo.json" with
      """
      {
        "parentDirectory":"{@systemTempPath}",
        "authorName":"GeoGig User",
        "authorEmail":"geogig@geogig.org"
      }
      """
    Then the response status should be '409'
    And the response ContentType should be "application/json"
    And the json object "response.success" equals "false"
    And the json object "response.error" equals "The specified repository name is already in use, please try a different name"
