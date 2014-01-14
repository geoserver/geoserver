.. _rest_examples_ruby:

Ruby
====

The examples in this section use `rest-client <http://github.com/archiloque/rest-client>`_, a REST client for Ruby. There is also a project to create a GeoServer-specific REST client in Ruby: `RGeoServer <https://github.com/rnz0/rgeoserver>`_.

Once installed on a system, ``rest-client`` can be included in a Ruby script by adding ``require 'rest-client'``.

GET and PUT Settings
--------------------

This example shows how to read the settings using GET, make a change and then use PUT to write the change to the server.

.. code-block:: ruby

  require 'json'
  require 'rest-client'

  url = 'http://admin:geoserver@localhost:8080/geoserver/rest/'

  # get the settings and parse the JSON into a Hash
  json_text = RestClient.get(url + 'settings.json')
  settings = JSON.parse(json_text)

  # settings can be found with the appropriate keys
  global_settings = settings["global"]
  jai_settings = global_settings["jai"]

  # change a value
  jai_settings["allowInterpolation"] = true

  # put changes back to the server
  RestClient.put(url + 'settings, settings.to_json, :content_type => :json)
