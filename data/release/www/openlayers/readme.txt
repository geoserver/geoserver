These files come from the OpenLayers distribution.

Originally, they were an svn link (using svn:externals),
but now we're shipping with the compressed OpenLayers.js
that can be found here:
http://trac.openlayers.org/wiki/HowToDownload

The img/ and themes/ directories are verbatim copies of
the OpenLayers distribution.

At the time of writing we're shipping with OpenLayers 2.8

Update this file every time you upgrade to track what's the
current version of OpenLayers we ship with.

This compressed build is a build of the full OpenLayers library.  
For production environments, it will likely be appropriate to use
a smaller build profile.

For information on building OpenLayers, see
http://trac.openlayers.org/wiki/Profiles