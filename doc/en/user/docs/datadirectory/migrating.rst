.. _datadir_migrating:

Migrating a data directory between versions
===========================================

It is possible to keep the same data directory while migrating to different versions of GeoServer, such as during an update.

There should generally be no problems or issues migrating data directories between patch versions of GeoServer (for example, from 2.9.0 to 2.9.1 or vice versa).

Similarly, there should rarely be any issues involved with migrating between minor versions (for example, from 2.8.x to 2.9.x). **Care should be taken to back up the data directory prior to migration.**

.. note:: Some minor version migrations may not be reversible, since **newer versions of GeoServer may make backwards-incompatible changes** to the data directory.
