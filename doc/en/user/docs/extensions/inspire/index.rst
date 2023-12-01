.. _community_inspire:

INSPIRE
=======

The INSPIRE extension allows GeoServer to be compliant with the View Service and Download Service specifications put forth by the **Infrastructure for Spatial Information in the European Community** (INSPIRE) directive.

In practice this means adding some extra elements into an extended capabilities section of the WMS, WFS and WCS capabilities documents. For WMS, WFS and WCS this includes a **Metadata URL** element with a link to the metadata associated with the service, and **SupportedLanguages** and **ResponseLanguage** elements which report the response language (GeoServer can only support one response language). For WFS and WCS there are also one or more **SpatialDataSetIdentifier** elements for each spatial data resource served by the service.

.. note:: The current INSPIRE extension fulfills "Scenario 1" of the View Service extended metadata requirements.  "Scenario 2" is not currently supported in GeoServer, but is certainly possible to implement.  If you are interested in implementing or funding this, please raise the issue on the :ref:`GeoServer mailing list <getting_involved>`.

For more information on the INSPIRE directive, please see the European Commission's `INSPIRE website <http://inspire.ec.europa.eu/>`_.

.. toctree::
   :maxdepth: 1 

   installing
   using


