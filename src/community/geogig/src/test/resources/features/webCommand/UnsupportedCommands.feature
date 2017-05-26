Feature: Unsupported Commands
  Some commands may be unsupported by the plugin.

  Scenario: Calling rename issues a 400 status code
    Given There is a default multirepo server
     When I call "POST /repos/repo1/rename?name=renamedRepo"
     Then the response status should be '400'
      And the response ContentType should be "application/xml"
      And the xpath "/response/success/text()" equals "false"
      And the xpath "/response/error/text()" equals "This command is unsupported by the GeoGig plugin."

  Scenario: Calling rename with json format issues a 400 status code
    Given There is a default multirepo server
     When I call "POST /repos/repo1/rename.json?name=renamedRepo"
     Then the response status should be '400'
      And the response ContentType should be "application/json"
      And the json object "response.success" equals "false"
      And the json object "response.error" equals "This command is unsupported by the GeoGig plugin."