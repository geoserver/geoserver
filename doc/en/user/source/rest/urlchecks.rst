.. _rest_urlchecks:

URL Checks
==========

This REST API allows you to create and manage URL External Access Checks in GeoServer.

.. note:: Read the :api:`API reference for /urlchecks <urlchecks.yaml>`.

Listing all URL Checks
----------------------

**List all URL Checks on the server, in JSON format:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/urlchecks.json

*Response*

.. code-block:: json

   {"urlchecks":{"urlcheck":[
        {"name":"external","href":"http:\/\/localhost:8080\/geoserver\/rest\/urlchecks\/external.json"},
        {"name":"icons","href":"http:\/\/localhost:8080\/geoserver\/rest\/urlchecks\/icons.json"},
        {"name":"safeWFS","href":"http:\/\/localhost:8080\/geoserver\/rest\/urlchecks\/safeWFS.json"}]}}


**List all URL Checks, in XML format:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/urlchecks.xml

*Response*

.. code-block:: xml

        <urlChecks>
            <urlCheck>
                <name>external</name>
                <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/urlchecks/external.xml" type="application/atom+xml"/>
            </urlCheck>
            <urlCheck>
                <name>icons</name>
                <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/urlchecks/icons.xml" type="application/atom+xml"/>
            </urlCheck>
            <urlCheck>
                <name>safeWFS</name>
                <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/urlchecks/safeWFS.xml" type="application/atom+xml"/>
            </urlCheck>
        </urlChecks>


Listing URL Check details
-------------------------

**Retrieve information about a specific URL Check:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/urlchecks/icons.xml

*Response*

.. code-block:: xml

        <regexUrlCheck>
            <name>icons</name>
            <description>External graphic icons</description>
            <enabled>true</enabled>
            <regex>^https://styles.server.net/icons/.*$</regex>
        </regexUrlCheck>


Creating a URL Check
--------------------

**Create a new URL Check:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XPOST -H "Content-type: text/xml" \
     -d "<regexUrlCheck> \
            <name>icons</name> \
            <description>External graphic icons</description> \
            <enabled>true</enabled> \
            <regex>^https://styles\.server\.net/icons/.*$</regex> \
        </regexUrlCheck>" \
     http://localhost:8080/geoserver/rest/urlchecks

*Response*

::

   201 Created

Changing an existing URL Check
------------------------------

**Edit the configuration of an existing URL Check:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XPUT -H "Content-type: text/xml" \
     -d "<regexUrlCheck> \
            <description>External graphic icons (disabled) </description> \
            <enabled>false</enabled> \
            <regex>^https://styles\.server\.com/icons/.*$</regex> \
        </regexUrlCheck>" \
     http://localhost:8080/geoserver/rest/urlchecks/icons

*Response*

::

   200 OK

Deleting a URL Check
--------------------

**Remove a URL Check:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XDELETE http://localhost:8080/geoserver/rest/urlchecks/icons

*Response*

::

   200 OK
