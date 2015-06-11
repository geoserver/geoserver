.. _versioning:

Versioning 
==========

Versioning refers to the ability of storing the history of the changes occurred to a feature, along with the user that performed the change and an optional comment, performing point in time extractions of the values and eventually rolling back the changes to a previous state.

GeoServer uses the GeoTools versioning PostGIS data store to provide two different kinds of versioning services:

* versioned WFS
* GeoServer Synchronization Service, a distributed version control system managing synchronization between a number of nodes, called unites, and a central orchestrating node called central

Versioned WFS
--------------

Versioned WFS is an extension of standard WFS that uses a versioned store and provides extra requests to deal with history, point in time extraction and rollbacks.
The following documents provide insight on its design, current implementation status and usage:

.. toctree::
   :maxdepth: 2

   wfsv/requirements/
   wfsv/classification/
   wfsv/implementations/
   wfsv/implementation/

GeoServer Synchronization Service
----------------------------------

GeoServer Synchronization Service is a distributed version control system managing synchronization between a number of nodes, called unites, and a central orchestrating node called central. 
The following documents provide insight on its design, current implementation status and usage:

.. toctree::
   :maxdepth: 2

   gss/introduction/
   gss/protocol/
   gss/primarykey/
   gss/dbschema/
   gss/limitations/