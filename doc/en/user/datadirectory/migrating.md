# Migrating a data directory between versions

It is possible to keep the same data directory while migrating to different versions of GeoServer, such as during an update or upgrade:

 -  **Care should be taken to back up the data directory prior to migration.**

 -  There should generally be no problems updating data directories with an incremental release of GeoServer (for example, from 2.28.0 to 2.28.1).

    It is also generally possible to revert a patch update and maintain data directory compatibility (for example, from GeoServer 2.28.1 to 2.28.0).

 -  There should rarely be any issues involved with upgrading between minor versions (for example, from 2.27.x to 2.28.x).
 
    Always check to [Upgrading GeoServer](../installation/upgrade3.md) page for notes on upgrading between different versions. This provides specific guidance for any manual steps required and notes any incompatibilities.

!!! note

    Check updating page as some minor version upgrades may not be reversible, since **newer versions of GeoServer may make backwards-incompatible changes** to the data directory.

 -  Upgrading between major versions of GeoServer (for example from 2.28 to 3.0) may not always be reversible, since newer versions of GeoServer may make backwards-incompatible changes to the data directory.
  
    Always check to [Upgrading GeoServer](../installation/upgrade3.md) page for notes on upgrading between different versions. This provides specific guidance for any manual steps required and notes any incompatibilities.

!!! note

    In the specifc case of GeoServer 2.28 to GeoServer 3.0 contact information `title` was added to customize the global welcome page or workspace welcome page. This setting is new for GeoServer 3.0, and is ignored if you downgrade to GeoServer 2.28.0.
