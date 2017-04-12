.. _rest_examples_php:

PHP
===

The examples in this section use the server-side scripting language `PHP <http://php.net/index.php/>`_, a popular language for dynamic webpages. PHP has `cURL functions <http://php.net/manual/en/ref.curl.php/>`_ , as well as 
`XML functions <http://www.php.net/manual/en/refs.xml.php/>`_, making it a convenient method for performing batch processing through the GeoServer REST interface. The following scripts execute single requests, but can be easily modified with looping structures to perform batch processing.

POST with PHP/cURL
------------------

The following script attempts to add a new workspace.

.. code-block:: php

  <?php 
      // Open log file
      $logfh = fopen("GeoserverPHP.log", 'w') or die("can't open log file");

      // Initiate cURL session
      $service = "http://localhost:8080/geoserver/"; // replace with your URL
      $request = "rest/workspaces"; // to add a new workspace
      $url = $service . $request; 
      $ch = curl_init($url);

      // Optional settings for debugging
      curl_setopt($ch, CURLOPT_RETURNTRANSFER, true); //option to return string
      curl_setopt($ch, CURLOPT_VERBOSE, true);
      curl_setopt($ch, CURLOPT_STDERR, $logfh); // logs curl messages

      //Required POST request settings
      curl_setopt($ch, CURLOPT_POST, True);
      $passwordStr = "admin:geoserver"; // replace with your username:password
      curl_setopt($ch, CURLOPT_USERPWD, $passwordStr);

      //POST data
      curl_setopt($ch, CURLOPT_HTTPHEADER, 
                array("Content-type: application/xml")); 
      $xmlStr = "<workspace><name>test_ws</name></workspace>";
      curl_setopt($ch, CURLOPT_POSTFIELDS, $xmlStr);
      
      //POST return code 
      $successCode = 201;

      $buffer = curl_exec($ch); // Execute the curl request
      
      // Check for errors and process results
      $info = curl_getinfo($ch);
      if ($info['http_code'] != $successCode) {
        $msgStr = "# Unsuccessful cURL request to ";
        $msgStr .= $url." [". $info['http_code']. "]\n";
        fwrite($logfh, $msgStr);
      } else {
        $msgStr = "# Successful cURL request to ".$url."\n";
        fwrite($logfh, $msgStr);
      }
      fwrite($logfh, $buffer."\n");

      curl_close($ch); // free resources if curl handle will not be reused
      fclose($logfh);  // close logfile

  ?>

The logfile should look something like::

  * About to connect() to www.example.com port 80 (#0)
  *   Trying 123.456.78.90... * connected
  * Connected to www.example.com (123.456.78.90) port 80 (#0)
  * Server auth using Basic with user 'admin'
  > POST /geoserver/rest/workspaces HTTP/1.1
  Authorization: Basic sDsdfjkLDFOIedlsdkfj
  Host: www.example.com
  Accept: */*
  Content-type: application/xml
  Content-Length: 43
  
  < HTTP/1.1 201 Created
  < Date: Fri, 21 May 2010 15:44:47 GMT
  < Server: Apache-Coyote/1.1
  < Location: http://www.example.com/geoserver/rest/workspaces/test_ws
  < Content-Length: 0
  < Content-Type: text/plain
  < 
  * Connection #0 to host www.example.com left intact
  # Successful cURL request to http://www.example.com/geoserver/rest/workspaces
  
  * Closing connection #0

If the cURL request fails, a code other than 201 will be returned.
Here are some possible values:

+------------+---------------------------------------------------------------+ 
| Code       |   Meaning                                                     |
+============+===============================================================+ 
|   0        |  Couldn't resolve host; possibly a typo in host name          |
+------------+---------------------------------------------------------------+ 
| 201        |  Successful POST                                              |
+------------+---------------------------------------------------------------+ 
| 30x        |  Redirect; possibly a typo in the URL                         |
+------------+---------------------------------------------------------------+ 
| 401        |  Invalid username or password                                 |
+------------+---------------------------------------------------------------+ 
| 405        |  Method not Allowed: check request syntax                     |
+------------+---------------------------------------------------------------+ 
| 500        |  GeoServer is unable to process the request,                  |
|            |  e.g. the workspace already exists, the xml is malformed, ... |
+------------+---------------------------------------------------------------+ 

For other codes see `cURL Error Codes <http://curl.haxx.se/libcurl/c/libcurl-errors.html>`_ and `HTTP Codes <http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html>`_.

GET with PHP/cURL
-----------------

The script above can be modified to perform a GET request to obtain
the names of all workspaces by replacing the code blocks for required
settings, data and return code with the following:

.. code-block:: php

  <?php
      // Required GET request settings
      // curl_setopt($ch, CURLOPT_GET, True); // CURLOPT_GET is True by default

      //GET data
      curl_setopt($ch, CURLOPT_HTTPHEADER, array("Accept: application/xml"));
      
      //GET return code 
      $successCode = 200;
  ?>

The logfile should now include lines like::

  > GET /geoserver/rest/workspaces HTTP/1.1
  
  < HTTP/1.1 200 OK

DELETE with PHP/cURL
--------------------

To delete the (empty) workspace we just created, the script is modified as follows:

.. code-block:: php

  <?php
      $request = "rest/workspaces/test_ws"; // to delete this workspace
  ?>

.. code-block:: php

  <?php
      //Required DELETE request settings
      curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "DELETE");
      $passwordStr = "admin:geoserver"; // replace with your username:password
      curl_setopt($ch, CURLOPT_USERPWD, $passwordStr);

      //DELETE data
      curl_setopt($ch, CURLOPT_HTTPHEADER, 
                array("Content-type: application/atom+xml")); 

      //DELETE return code 
      $successCode = 200;
  ?>

The log file will include lines like::

  > DELETE /geoserver/rest/workspaces/test_ws HTTP/1.1

  < HTTP/1.1 200 OK
 
