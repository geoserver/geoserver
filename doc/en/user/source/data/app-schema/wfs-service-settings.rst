.. _app-schema.wfs-service-settings:

WFS Service Settings
====================

There are two GeoServer WFS service settings that are strongly recommended for interoperable complex feature services. These can be enabled through the :menuselection:`Services --> WFS` page on the GeoServer web interface or by manually editing the :file:`wfs.xml` file in the data directory,

Canonical schema location
-------------------------

The default GeoServer behaviour is to encode WFS responses that include a ``schemaLocation`` for the WFS schema that is located on the GeoServer instance. A client will not know without retrieving the schema whether it is identical to the official schema hosted at ``schemas.opengis.net``. The solution is to encode the ``schemaLocation`` for the WFS schema as the canonical location at ``schemas.opengis.net``.

To enable this option, choose *one* of these:

#. Either: On the :menuselection:`Service --> WFS` page under *Conformance* check *Encode canonical WFS schema location*.
#. Or: Insert the following line before the closing tag in :file:`wfs.xml`::

    <canonicalSchemaLocation>true</canonicalSchemaLocation>

Encode using featureMember
--------------------------

By default GeoServer will encode WFS 1.1 responses with multiple features in a single ``gml:featureMembers`` element. This will cause invalid output if a response includes a feature at the top level that has already been encoded as a nested property of an earlier feature, because there is no single element that can be used to encode this feature by reference. The solution is to encode responses using ``gml:featureMember``.

To enable this option, choose *one* of these:

#. Either: On the :menuselection:`Service --> WFS` page under *Encode response with* select *Multiple "featureMember" elements*.
#. Or: Insert the following line before the closing tag in :file:`wfs.xml`::

    <encodeFeatureMember>true</encodeFeatureMember>


