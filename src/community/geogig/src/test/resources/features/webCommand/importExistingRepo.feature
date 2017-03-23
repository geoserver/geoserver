# (c) 2017 Open Source Geospatial Foundation - all rights reserved
# This code is licensed under the GPL 2.0 license, available at the root
# application directory.

Feature: Import an Existing GeoGig repository
  The import of a repository is done by issuing a POST request
  to "/repos/<REPO_NAME>/importExistingRepo" API (for an XML response)
  and "/repos/<REPO_NAME>/importExistingRepo.json" API (for a JSON response).
  The response should be a 200 OK and contain the name of the new repository as well as
  an href URL.

  @newtest
  Scenario: Import of non existent repository fails
    Given There is a default multirepo server
    Given A repository named "geogigRepo" is initialized
    When I post content-type "application/json" to "/repos/nonExistentRepo/importExistingRepo" with
      """
      {
        "parentDirectory":"{@systemTempPath}",
        "leafDirectory":"nonExistentRepo",
        "authorName":"GeoGig User",
        "authorEmail":"geogig@geogig.org"
      }
      """
    Then there should be no "nonExistentRepo" created
    And the response body should contain "Repository not found"

  @newtest
  Scenario: Import of existent repository succeeds with an XML response
    Given There is a default multirepo server
    Given A repository named "geogigRepo" is initialized
    When I post content-type "application/json" to "/repos/geogigRepo/importExistingRepo" with
      """
      {
        "parentDirectory":"{@systemTempPath}",
        "leafDirectory":"geogigRepo",
        "authorName":"GeoGig User",
        "authorEmail":"geogig@geogig.org"
      }
      """
    Then the response status should be '200'
    And the response body should contain "http://localhost:8080/geoserver/geogig/repos/geogigRepo.xml"

  @newtest
  Scenario: Import of existent repository succeeds with a JSON response
    Given There is a default multirepo server
    Given A repository named "geogigRepo" is initialized
    When I post content-type "application/json" to "/repos/geogigRepo/importExistingRepo.json" with
      """
      {
        "parentDirectory":"{@systemTempPath}",
        "leafDirectory":"geogigRepo",
        "authorName":"GeoGig User",
        "authorEmail":"geogig@geogig.org"
      }
      """
    Then the response status should be '200'
    And the response body should contain "http://localhost:8080/geoserver/geogig/repos/geogigRepo.json"
