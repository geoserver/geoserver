.. _wfsv_extension:

WFS Versioning
==============

Introduction
************

One of GeoServer's goals is to help bring the types of collaboration of open source software to the geospatial domain.  Just like people across the world form communities and governance structures to build software together, we hope to enable similar things to happen with the creation and maintenance of geospatial data.  In the software domain there are a variety of tools - IDE's, version control, bug trackers, etc. that help make this possible.  GeoServer hopes to help provide tools that better enable geospatial collaboration.

The Transaction portion of the WFS Standard (also known as WFS-T) specifies an open protocol for inserting, deleting and updating geospatial information.  Which is a great start to enable a wide variety of tools, both web-based and desktop, to edit the same database.  But it falls short in all but the most controlled environments, as it's too easy to mess things up for everyone.  GeoServer's :ref:`security` system is one step in this direction, to help people control their environment.  

WFS Versioning is a set of extensions to the WFS protocol to keep track of 'versions' of edits.  This enables wiki-style editing of geospatial information, by keeping the history of all changes to the data.


Protocol
**********
WFS Versioning (WFS-V) is **not** an official OGC standard, and has not yet entered the standard process.  In time the GeoServer community hopes to try to standardize it, but first is focused on getting real world implementations.  It is designed to be as compatible with WFS-T as possible, reusing elements and extending operations.  In the future a REST equivalent may be implemented.

WFS-V adds two new operations and one new action on the Transaction operation.  

.. list-table::
   :widths: 20 80

   * - **Operation**
     - **Description**
   * - ``GetLog``
     - Returns summaries of the changes that have taken place over a set of constraints.
   * - ``GetDiff``
     - Retrieves the actual changes that have occurred.
   * - ``Rollback`` (optional)
     - A convenience Transaction element, to revert changes to a previous revision.

For more information on the extensions to WFS see the detailed 
`draft specification <http://geoserver.org/display/GEOS/Versioning+WFS+-+Extensions>`_.



Implementation
**************

GeoServer has completed a first phase implementation that is working in prototype situations.  See 
`this blog post <http://blog.opengeo.org/2009/03/17/versioning-vespucci/>`_ for more information on one of the working prototypes.  It has not been used in production, so don't expect it to work perfectly.  But the extension is available for download, and we appreciate any help we can get, even just trying it out and reporting bugs.  

The implementation currently just works against PostGIS.  But there are future plans to have it work seamlessly with Oracle Workspace Manager and ArcSDE Versioning.  Doing this may involve adjusting the protocol.  On the client side the protocol has been implemented in OpenLayers.  And it will work transparently against any WFS-T client, though a non-versioning aware client won't be able to supply commit messages or make use of the advanced operations.  But all changes it does make will be versioned.

See the `Phase one implementation proposal <http://geoserver.org/display/GEOS/Versioning+WFS+-+Phase+one+implementation+proposal>`_ section of the wiki for more information on what's been built.  The main
`Versioning WFS <http://geoserver.org/display/GEOS/Versioning+WFS>`_ page also has a lot of information on the background and decisions in the implementation, and will be the point of collaboration in the future.

Trying it out
*************

After the next round of work on WFS-V we will be publishing some docs to get anyone started.  In the meantime advanced users can likely figure things out by looking at some of the older information on the wiki - `trying early WFS-V prototype <http://geoserver.org/display/GEOS/Trying+out+the+early+WFS-V+prototype>`_ and the `Versioning <http://geoserver.org/display/GEOSDOC/6+Versioning>`_  section from the 2007 Foss4g workshop.  