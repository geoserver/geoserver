.. _ogcapi_links:

Configuring the GeoServer OGC API module
========================================

The OGC API module is mostly using the same configurations as the equivalent OWS services.
So, for example, setting the WFS limited SRS list will also limit the SRS list for OGC API - Features.

The OGC API module is also using the same security configuration as the equivalent OWS services.

In addition, the OGC API module has some unique configuration options, explained below.

Custom links for the "collections" resource
-------------------------------------------

By specification, the ``collections`` resource can have a number of additional links, beyond
the basic ones that the service code already includes. 

The administrator can add links for this resource either in the Global pages, or in the
workspace specific settings:

  .. figure:: img/links.png
 
     Links editor

Link editor column description:

* **rel**: the link relation type, as per the OGC API - Features specification
* **Mime type**: the mime type for the resource found following the link
* **URL**: the link URL
* **Title**: the link title (optional)
* **Service**: the service for which the link is valid (optional, defaults to all) 


Common links relationships that could be added for the ``collections`` resource are:

* ``enclosure``, in case there is a package delivering all the collections (e.g. a GeoPackage, a ZIP full of shapefiles).
* ``describedBy``, in case there is a document describing all the collections (e.g. a JSON or XML schema).
* ``license``, if all collection data is under the same license.

Custom links for single collections
-----------------------------------

By specification, the ``collection`` resource can have a number of additional links, beyond
the basics ones that the service code already includes.

The editor is the same, but is found in the "Publishing" tab of the layer in question.
The relationships are the same as for the ``collections`` resource, but used in case
there is anything that is specific to the collection (e.g., the schema for the single collection). 
In addition, other relations can be specified, like the ``tag`` relation, to link to the eventual
INSPIRE feature concept dictionary entry.