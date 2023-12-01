.. _rest_fonts:

Fonts
=====

The REST API allows you to list—but not modify—fonts available in GeoServer. It can be useful to use this operation to verify if a font used in a style file is available before uploading it.

.. note:: Read the :api:`API reference for /fonts <fonts.yaml>`.

Getting a list of all fonts
---------------------------

**List all fonts on the server, in JSON format:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/fonts.json

*Response*

.. code-block:: json

   {"fonts":["Calibri Light Italic","Microsoft PhagsPa Bold","Lucida Sans Typewriter Oblique","ChaparralPro-Regular","Californian FB Italic"]}


**List all fonts on the server, in XML format:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/fonts.xml

*Response*

.. code-block:: xml

              <root>
                <fonts>
                  <entry>Calibri Light Italic</entry>
                  <entry>Microsoft PhagsPa Bold</entry>
                  <entry>Lucida Sans Typewriter Oblique</entry>
                  <entry>ChaparralPro-Regular</entry>
                  <entry>Californian FB Italic</entry>
                </fonts>
              </root>

