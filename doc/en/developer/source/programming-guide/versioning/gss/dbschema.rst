.. _versioning_gss_dbschema:

Database tables
================

Central management tables
-------------------------

Central needs to maintain a list of all the units available around in order to schedule and perform the updates. 

.. list-table:: UNITS
   :widths: 20 20 80
   :header-rows: 1

   * - Colum
     - Type
     - Description
   * - UnitId
     - Long
     - A unique code that identifies the unit
   * - Name
     - Varchar(256)
     - The name of the management unit. Used in reports and in the connectivity map.
   * - Address
     - Varchar(1024)
     - Full URL of the GSS service hosted on the Unit (as http://host:port/path/to/gss, usually http://host:8080/geoserver/gss)
   * - User
     - Varchar(256)
     - The user name used to log into the unit's GSS to perform synchronization (null if no basic authentication is required)
   * - Password
     - Varchar(256)
     - The password used to log into the unit's GSS to perform synchronization. 
   * - TimeStart
     - Time
     - Beginning of the contact window
   * - TimeEnd
     - Time
     - End of the contact window
   * - Interval
     - Integer
     - Synchronisation interval (how many minutes to wait between one synchronization cycle and the next)
   * - AreaOfInterest
     - Polygon
     - The geographic area managed by the unit (used for the connectivity map)
   * - Errors
     - Boolean
     - Flag raised when the last synchronization failed

Central also need to maintain a list of tables that need to be synchronized with each unit.

.. list-table:: UNIT_TABLES
   :widths: 20 20 80
   :header-rows: 1

   * - Column
     - Type
     - Description
   * - UnitId
     - Long
     - A unique code that identifies the unit
   * - TableId
     - Long
     - The identifier of the table to be synchronized
   * - LastSynchronization
     - Time
     - Time of the last successful synchronization
   * - LastFailure
     - Time
     - Time of the last failed synchronisation
   * - LastCentralRevision
     - Long
     - Central revision number at the last successful synchronization 
   * - LastUnitRevision
     - Long
     - Unit revision at the last successful synchronization

Finally each table needs to be described:

.. list-table:: SYNCH_TABLES
   :widths: 20 20 80
   :header-rows: 1

   * - Column
     - Type
     - Description
   * - TableId
     - Long
     - Identifier
   * - Name
     - Varchar(256)
     - The table name
   * - Type
     - Char(1)
     - The type of table: P (published), B (backup), 2 (two way synchronized)

Since the system design does not take into consideration the creation of new tables there won't be a need to synchronize the tables with the clients.

Unit management tables
----------------------
Each unit also needs to keep track of the layers to be synchronized:

.. list-table:: SYNCH_TABLES
   :widths: 20 20 80
   :header-rows: 1

   * - Column
     - Type
     - Description
   * - TableId
     - Long
     - Identifier
   * - Name
     - Varchar(256)
     - The table name
   * - Type
     - Char(1)
     - The type of table: P (published), B (backup), 2 (two way synchronized)

The following table records the synchronization history and allows to generate a clean GetDiff response to Central:

.. list-table:: SYNCH_TABLES
   :widths: 20 20 80
   :header-rows: 1

   * - Column
     - Type
     - Description
   * - TableName
     - Varchar(256)
     - The table to be synchronized
   * - LocalRevision
     - Long
     - The local revision number at which the Central PostDiff has been committed (might be null if the Central difference document was empty)
   * - CentralRevision
     - Long
     - The central revision number included in the PostDiff operation

.. list-table:: SYNCH_CONFLICTS
   :widths: 20 20 80
   :header-rows: 1

   * - Column
     - Type
     - Description
   * - TableName
     - Varchar(256)
     - The table containing the conflicting feature
   * - FeatureId
     - Long
     - The identifier of the conflicting feature
   * - LocalRevision
     - Long
     - The local revision before the central incoming change generated the conflict
   * - Resolved
     - Boolean
     - Conflict resolution flag
   * - Difference
     - Text
     - The difference that would bring the current central feature to the one edited locally
