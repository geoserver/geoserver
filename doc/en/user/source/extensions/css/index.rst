CSS Styling
===========

The ``css`` module for GeoServer adds an alternative style editor to GeoServer
that uses a CSS-derived language instead of SLD.  These CSS styles are
internally converted to SLD, which is then used as normal by GeoServer.
The CSS syntax is duplicated from SVG styling where appropriate, but extended
to avoid losing facilities provided by SLD when possible.  As an example, it
provides facilities for extracting feature attributes to use in labelling,
sizing point markers according to data values, etc. 

Read on for information about: 

.. toctree:: 
   :maxdepth: 1

   install
   tutorial
   filters
   metadata
   multivalues
   properties
   values
   styled-marks
   cookbook
   examples
