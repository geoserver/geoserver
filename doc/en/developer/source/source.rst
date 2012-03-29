.. _source:

Source Code
===========

The GeoServer source code is located at http://svn.codehaus.org/geoserver. 

To check out the development / trunk version::

  svn co http://svn.codehaus.org/geoserver/trunk geoserver

To check out the stable / branch version::

  svn co http://svn.codehaus.org/geoserver/branches/2.1.x geoserver 

.. warning::

   The GeoServer repository contains a significant amount of spatial data. 
   Checking it out over a slow or low bandwidth connection can be costly. In 
   such cases it may be desirable to check out only the sources:: 

       svn co http://svn.codehaus.org/geoserver/trunk/src 


Committing
----------

In order to commit to the repository the following steps must be taken:

#. Install this subversion :download:`config` file. See additional notes below.
#. Register for commit access as described here.
#. Switch the repository to the "https" protocol. Example::

     [root of checkout]% svn switch --relocate http://svn.codehaus.org/ https://svn.codehaus.org/ .

Repository structure
--------------------

::

  http://svn.codehaus.org/geoserver/
     branches/
     spike/
     tags/
     trunk/

* ``branches`` contains all previously stable development branches, 1.6.x, 
  1.7.x, etc...
* ``spike`` contains experimental projects and mock ups
* ``tags`` contains all previously released versions
* ``trunk`` is the current development branch

Branch structure
----------------

Each development branch has the following structure::

  http://svn.codehaus.org/geoserver/
     doc/
     src/
     data/

* ``doc`` contains the sources for the user and developer guides 
* ``src`` contains the java sources for GeoServer itself
* ``data`` contains a variety of GeoServer  data directories 

