.. _wcs_vendor_parameters:

WCS Vendor Parameters
=====================

Requests to the WCS GetCapabilities operation can be filtered to only return layers corresponding to a particular namespace.

Sample code:

.. code-block:: xml

   http://example.com/geoserver/wcs?
      service=wcs&
      version=1.0.0&
      request=GetCapabilities&
      namespace=topp

Using an invalid namespace prefix will not cause any errors, but the document returned will not contain information on any layers.