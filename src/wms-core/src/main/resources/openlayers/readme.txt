These files come from the OpenLayers distribution.

Originally, they were an svn link (using svn:externals),
but now we're shipping with the compressed OpenLayers.js
that can be found here:
http://trac.openlayers.org/wiki/HowToDownload

The themes/ directory is verbatim copy of
the OpenLayers distribution, img/ comes from the OL distribution.

At the time of writing we're shipping with OpenLayers 2.8

Update this file every time you upgrade to track what's the
current version of OpenLayers we ship with.

This compressed build is a build of a select portion of the OpenLayers library.  
For production environments, you may have to use a different build profile.
The build profile used here is included in this same directory (wms.cfg).

For information on building OpenLayers, see
http://trac.openlayers.org/wiki/Profiles