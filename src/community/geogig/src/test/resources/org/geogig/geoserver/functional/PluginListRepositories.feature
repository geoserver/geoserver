# (c) 2016 Open Source Geospatial Foundation - all rights reserved
# This code is licensed under the GPL 2.0 license, available at the root
# application directory.

@PluginRepositoryManagement
Feature: Plugin list repositories
  Listing all repositories on the server is done by issuing a GET request to "/repos".
  The response should be a 200 OK and contain a list of repositories configured. For each repository
  in the returned list, the NAME, ID a a link to the repository's info should be included.

  Scenario: Get list of Repositories in JSON format
    Given There is a default multirepo server
     When I call "GET /repos.json"
     Then the response status should be '200'
      And the response ContentType should be "application/json"
      And the json response "repos.repo" should contain "id" 2 times
      And the json response "repos.repo" should contain "name" 2 times
      And the json response "repos.repo" should contain "href" 2 times
      And the json response "repos.repo" attribute "href" should each contain "/geoserver/geogig/repos/"

  Scenario: Get Repository info in JSON format
    Given There is a default multirepo server
     When I call "GET /repos.json"
     Then the response status should be '200'
      And the response ContentType should be "application/json"
      And the json response "repos.repo" should contain "href" 2 times
     Then I save the first href link from "repos.repo" as "@href"
     When I call "GET {@href}"
     Then the response status should be '200'
      And the response ContentType should be "application/json"
      And the json response "repository" should contain "id"
      And the json response "repository" should contain "name"
      And the json response "repository" should contain "location"
     
  Scenario: Get Repository info in XML format
    Given There is a default multirepo server
     When I call "GET /repos"
     Then the response status should be '200'
      And the response ContentType should be "application/xml"
      And the xml response should contain "/repos/repo/atom:link" 2 times
     
