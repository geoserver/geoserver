.. _app-schema.gml32:

Supported GML Versions
======================


GML 3.1.1
---------

* GML 3.1.1 application schemas are supported for WFS 1.1.0.
* Clients must specify WFS 1.1.0 in requests because the GeoServer default is WFS 2.0.0.
* GET URLs must contain ``version=1.1.0`` to set the WFS version to 1.1.0.


GML 3.2.1
---------

* GML 3.2.1 application schemas are supported for WFS 1.1.0 and (incomplete) WFS 2.0.0.
* Some WFS 2.0.0 features not in WFS 1.1.0 such as GetFeatureById are not yet supported.
* Clients using WFS 1.1.0 must specify WFS 1.1.0 in requests and select the ``gml32`` output format for GML 3.2.1.
* To use WFS 1.1.0 for GML 3.2.1, GET URLs must contain ``version=1.1.0`` to set the WFS version to 1.1.0 and ``outputFormat=gml32`` to set the output format to GML 3.2.1.
* The default WFS version is 2.0.0, for which the default output format is GML 3.2.1.
* All GML 3.2.1 responses are contained in a WFS 2.0.0 ``FeatureCollection`` element, even for WFS 1.1.0 requests, because a WFS 1.1.0 ``FeatureCollection`` cannot contain GML 3.2.1 features.


Secondary namespace for GML 3.2.1 required
``````````````````````````````````````````

GML 3.2.1 WFS responses are delivered in a WFS 2.0.0 ``FeatureCollection``. Unlike WFS 1.1.0, WFS 2.0.0 does not depend explicitly on any GML version. As a consequence, the GML namespace is secondary and must be defined explicitly as a secondary namespace. See :ref:`app-schema.secondary-namespaces` for details.

For example, to use the prefix ``gml`` for GML 3.2, create ``workspaces/gml/namespace.xml`` containing::

    <namespace>
        <id>gml_namespace</id>
        <prefix>gml</prefix>
        <uri>http://www.opengis.net/gml/3.2</uri>
    </namespace>

and ``workspaces/gml/workspace.xml`` containing::

    <workspace>
        <id>gml_workspace</id>
        <name>gml</name>
    </workspace>

Failure to define the ``gml`` namespace prefix with a secondary namespace will result in errors like::

    java.io.IOException: The prefix "null" for element "null:name" is not bound.

while encoding a response (in this case one containing ``gml:name``), even if the namespace prefix is defined in the mapping file.


GML 3.2.1 geometries require gml:id
```````````````````````````````````

GML 3.2.1 requires that all geometries have a ``gml:id``. While GeoServer will happily encode WFS responses without ``gml:id`` on geometries, these will be schema-invalid. Encoding a ``gml:id`` on a geometry can be achieved by setting an ``idExpression`` in the mapping for the geometry property. For example, ``gsml:shape`` is a geometry property and its ``gml:id`` might be generated with::

    <AttributeMapping>
        <targetAttribute>gsml:shape</targetAttribute>
        <idExpression>
            <OCQL>strConcat('shape.', getId())</OCQL>
        </idExpression>
        <sourceExpression>
            <OCQL>SHAPE</OCQL>
        </sourceExpression>
    </AttributeMapping>

In this example, ``getId()`` returns the ``gml:id`` of the containing feature, so each geometry will have a unique ``gml:id`` formed by appending the ``gml:id`` of the containing feature to the string ``"shape."``.

If a multigeometry (such as a ``MultiPoint`` or ``MultiSurface``) is assigned a ``gml:id`` of (for example) ``parentid``, to permit GML 3.2.1 schema-validity, each geometry that the multigeometry contains will be automatically assigned a ``gml:id`` of the form ``parentid.1``, ``parentid.2``, ... in order.


GML 3.3
-------

The proposed GML 3.3 is itself a GML 3.2.1 application schema; preliminary testing with drafts of GML 3.3 indicates that it works with app-schema as expected.

