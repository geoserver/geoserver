.. _versioning_implementations_osm:

OpenStreetMap
=============

From the OpenStreetMap home page:

* OpenStreetMap is a project aimed squarely at creating and providing free geographic data such as street maps to anyone who wants them;
* Anyone can edit the current map using one of our map editors, or upload and share your GPS tracklogs. Before you can upload, or edit, you will need to create an account.

OpenStreetMap is one of a kind in many ways, from the feature concept, to geometry handling, versioning approach, and API. Let's analyze the most interesting parts of it.


Feature concept
---------------

Features in OpenStreetMap are tagged geometries, where tagging is completely open ended, a map from keys to values.
So, we have node, segment, ways and areas, where each record can have a different set of attributes.

This is very different from a WFS approach, where data is typed and has a foreseen number of attributes.
The pro is that any kind of information can be stored, the cons is that there is no type checking and you cannot handle data making assumptions on data structure (so no SLD or filters, for example).


Geometry concept
----------------

Geometries are not simple features, they are topologies:

    * Node is the basic concept, it's a point. A node can be shared by various segments (as start or end point).
    * Segment is a straight line between two nodes used to build ways and areas. Segments can be shared between various ways and areas;
    * Way is a set of segments.
    * Area is a set of segments as well. Apparently holes are not supported, not sure about multipolygons.

As said above, each can be tagged with extra information.

Versioning
----------

All data in OSM is apparently stored in a mysql database, whose schema is provided here: http://svn.openstreetmap.org/sql/mysql-schema.sql.
The schema is peculiar, there are no foreign keys or spatial indexes that I can see.

First thing you can note is that there's a split between a set of current_xxx tables and xxx tables: the former represent the current state of the database, the last revision of it, whilst xxx tables do represent the history.
This is evidently done to increase performance, since the most accessed data is the "live" one anyways.
Each table representing a feature (nodes, segments, areas, ways) has a user attribution, a time stamp, and a visibility flag.

A version attribute is found in tables related to areas and ways, but not in segments and nodes (current tables do not have a version number). It seems the system relies more on timestamps than version numbers to do its work?

Tag handling is mixed, for nodes and segments it's a single text field, whilst for area and ways the thing is handled with a normalized one to many approach.

API
---

API is based on a REST approach. The API is simple, a full description can be found here: http://wiki.openstreetmap.org/index.php/REST.

A few considerations here:

* all calls are authenticated using basic authentication, in order to provide attribution;
* there's no transaction concept, in order to add a new way you have perform three different requests, to separately add nodes, segments and areas. This seems to be quite fragile data consistency wise.
* there is no commit message support, neither dirty area support, so it's hard to tell what was done in each commit... this is consistent with the lack of transaction throughout.
* there's no explicit rollback operation, thought data structures are in place to support it?
* even if version number are handled internally, they are not shown in the xml format.

