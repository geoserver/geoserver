.. _app-schema.gml32:

GML 3.2
=======

GML 3.2 application schemas require additional configuration steps.


Secondary namespace for GML required
------------------------------------

GML 3.2 WFS responses are delivered in a WFS 2.0 ``FeatureCollection``. Unlike WFS 1.1, WFS 2.0 does not depend explicitly on any GML version. As a consequence, the GML namespace is secondary and must be defined explicitly as a secondary namespace. See :ref:`app-schema.secondary-namespaces` for details.

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


Geometries require gml:id
-------------------------

GML 3.2 requires that all geometries have a ``gml:id``. While GeoServer will happily encode WFS responses without ``gml:id`` on geometries, these will be schema-invalid. Encoding a ``gml:id`` on a geometry can be achieved by setting an ``idExpression`` in the mapping for the geometry property. For example, ``gsml:shape`` is a geometry property and its ``gml:id`` might be generated with::

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

If a multigeometry (such as a ``MultiPoint`` or ``MultiSurface``) is assigned a ``gml:id`` of (for example) ``parentid``, to permit GML 3.2 schema-validity, each geometry that the multigeometry contains will be automatically assigned a ``gml:id`` of the form ``parentid.1``, ``parentid.2``, ... in order.


