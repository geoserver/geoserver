.. _datadir_migrating:

Migrating a data directory between versions
===========================================

It is possible to keep the same data directory while migrating to different versions of GeoServer, such as during an update or upgrade:

* **Care should be taken to back up the data directory prior to migration.**

* There should generally be no problems or issues updating data directories between patch versions of GeoServer (for example, from 2.28.0 to 2.28.1 or vice versa).
  
  It is also generally possible to revert a minor update and maintain data directory compatibility.

* There should rarely be any issues involved with upgrading between minor versions (for example, from 2.27.x to 2.28.x).

  Always check to :ref:`installation_upgrade` page for notes on upgrading between different versions. This provides specific guidance for any manual steps required.

  .. note:: Some minor version upgrades may not be reversible, since **newer versions of GeoServer may make backwards-incompatible changes** to the data directory.

* Upgrading between major versions of GeoServer (for example from 2.28 to 3.0) may not be reversible,
  since newer versions of GeoServer may make backwards-incompatible changes to the data directory.