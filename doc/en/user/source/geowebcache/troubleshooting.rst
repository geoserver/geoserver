.. _gwc_troubleshooting:

Troubleshooting
===============

Sometimes errors will occur when requesting data from GeoWebCache.  Below are some of the most common reasons.

Grid misalignment
-----------------

Sometimes errors will occur saying that the "resolution is not supported" or the "bounds do not align."  This is due to the client making WMS requests that do not align with the grid of tiles that GeoWebCache has created, such as differing map bounds or layer bounds, or an unsupported resolution.  If you are using OpenLayers as a client, looking at the source code of the included demos may provide more clues to matching up the grid.

An alternative workaround is to set up GeoWebCache integration with the GeoServer WMS.  See the section on :ref:`gwc_seeding` for more details.
