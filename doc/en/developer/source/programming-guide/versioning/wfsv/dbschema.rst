.. _versioning_implementation_dbschema:

Datastore design
================

The solution is a mix between Oracle, ArcSDe, svn and the Chris proposal. From a classification point of view, it's a forward only versioning with no diffs, and no branches, using progressive revision numbers like svn.

Here is the diagram:

.. image: images/versioningNoBranches.jpg

The **Data** table represents a single versioned feature type, has all the attributes of the normal feature type, and a few extra columns:

* rowId: the primary key if the versioning was not there, backed by a sequence.
* revisionCreated: the revision where the row was introduced (as a result of an insert or of a row update).
* revisionExpired: the revision where the row was marked as "old" (as a result of a deletion or an update). For rows that are not expired, it's assumed to be MAXLONG (because, as you'll see, this eases querying).

The Data table primary key is (rowId, revisionCreated). RevisionExpired should be indexed as well.

The **ChangeSet** represents the change in a set of rows contained in versioned feature type as a result of a Transaction or rollback operation. The columns are:

* revision: backed by a sequence, it's the global revision number, backed by a sequence (primary key of this table).
* author: is the id/name/whatever of the person/system that performed the change.
* date: the full timestamp (milliseconds included) of the change.
* bbox: the area that the changes involved, expressed in ESPG:4326 (since different feature types may be expressed in different CRS).
* message: the commit message for the change.

Each changeset is associated to a list of involved tables, in order to ease finding the changed records for each changeset.

The **VersionedTable** table is a list containing the currently versioned feature types:

* versionedTableId: backed by a sequence, it's the primary key;
* name: the versioned table name

.. note: An alternate implementation using just one revision column containing either 0 for the current revision and the revision the row expired for other rows has been tried out in the benchmark belows, but has proved to be at least two orders of magnitude slower when trying to extract a specific revision

Common operations implementations in terms of SQL queries
----------------------------------------------------------

It's interesting to see how this design allows for relatively simple queries for the most common operations:

* Gather a specific revision, with filter::

      select *
      from data
      where <filter encoded in sql>
      and revisionCreated <= [revision]
      and revisionExpired > [revision]

* Last version, with filter::

      select max(revision)
      from changes

      and then the same as gathering a specific revision

* State at a certain point in time::

      select max(revision)
      from changes
      where date < [specifiedDate]

      and then the same as getting a specific revision.

* Rollback to a specific revision, but keeping history:
  First a set of intersting queries in order to undersand what's going on:
  All modified rows between revision and current, and matching a certain filter at some point during the evolution of that branch::

      select distinct(rowId) from data
      where revisionCreated  > [revision]
      and [filter]

  Original state of all rows existing at the specified revision (and matching the filter)::

      select data1.*
      from data data1
      and data1.revisionCreated <= [revision]
      and data1.revisionExpired > [revision]
      and data1.rowId in (
        select data2.rowId
        from data data2
        where	data2.revisionCreated > [revision]
        and [filter])

  Current state of all rows created and not deleted after the specified revision::

      select * from data
      where revisionCreated > [revision]
      and revisionExpired = [maxLong]
      and rowId not in (
       select rowId
       where data1.revisionExpired > [revision]
       and [filter])

  So basically reverting means running the following queries::

      -- update old revisions, equates to a deletion
      -- for those that were not there
      update data
      set revisionExpired = [newRevision]
      from data
      where revisionCreated > [revision]
      and revisionExpired = [maxLong]
      and rowid in  (
         select rowId 
         from data data2 
         where data2.expired > [revision]
         and [filter]
      )

      -- inserts the old values for all data that
      -- has been updated or deleted between the two  revisions
      insert into data
      select d.* (besides rev numbers), [newRevision], [maxLong]
      from data d
      where d.revisionCreated >= [revision]
      and d.revisionExpired > [revision]
      and d.rowId in (
       select data2.rowId
       from data data2
       where data2.revisionExpired > [revision]
       and [filter]
      )

* Diffing between revision n and revision m
  Last value of everything changed between n and m (m > n), and satisfying a filter in one of the past states (if row is not changed, it won't have old states between n and m)::

      select * 
      from data d1
      where revisionCreated <= m
      and revisionExpired >= m
      and rowId in (
        select rowid
        from data d2
        where d2.rowid = d1.rowid
        and revisionCreated < d1.revisionCreated
        and revisionExpired > n
        and [filter])
      order by rowId

  Value at n of everything changed between n and m (eventually deleted)::

      select *
      from data d1
      where revisionCreated <= n
      and revisionExpired > n
      and rowId in (
        select rowid
        from data d2
        where d2.rowid = d1.rowid
        and revisionCreated > d1.revisionCreated
        and [filter])
      order by rowId

  Then the two sets must be scanned in parallel like in a merge sort and diffs must be generated (the diff format is still a matter of discussion). This unfortunately works fine only for single column keys, for multicolumns it's not as obvious, especially if the primary key is allowed to change like in the multicolumn fid mappers. Hmmm... this must be forbidden in fact for identity chain to work (rowid is what keeps togheter the rows history...).

  Moreover, the discussion assumes filter can be encoded in sql, and it may not be the case... this complicates matters a lot since the filter is to be applied in a subquery that does not return values.

* Getting change history for a table, with eventual filter on area or user::

      select date, author, revision, message
      from ChangeSets
      where revision in (
         select revision
         from TablesChanged
         where versionedTableId = [tableId])
      and bbox && [searchArea]
      and user = [specifiedUser]

Performance tests
-----------------

One of the main concerns given the data structures we are setting up is scalability, that is, we do expect a performance hit due to versioning, and wonder how severe it is compared to revision numbers in the database and the actual modifications performed by each release.

A good implementation should degrade no worse than O, where n is the total number of versioned records. Well, to my surprire, it seems the above table setup, with proper indexes, is less than linear, but almost constant .

To asses performance I've setup a little data filler and then a query benchmark.
The data filler:

* sets up a spatial table in Postgis with a linestring geometry, a text attribute, a numeric id and revision columns (testData table);
* sets up a versioned spatial table with the same structure as the previous, but without the revision columns, as a reference of the performance that can be obtained without versioning around;
* inserts a certain amount of data in testData as the first revision, filling the lat/lon space with a regular grid (each feature occupies a cell). Geometries are random, but guaranteed to fit in their cell.
* starts versioning data modifiying for each revision a certain number of features, and marking as expired the current version of it;
* fill the reference data table with a snapshot of the last revision;
* does a vacuum analyze to make sure optimizer knows about data distribution.

The query benchmark instead performs a few queries against reference and versioned data:

* an extraction of the full data set from reference, and then last revision from versioned, and a few snapshots as specif versions;
* an extraction of a bbox (big enough to be timed), and the same against the last revision and specific versions.
      The above is run twice to make sure the are no caching effects around, and in fact, the second run does not seem to hit the disk at all, but runs against the file system cache.

Tests have been performed on an Intel Core 2 Duo 2.13Ghz, 2GB RAM, and two 7200 rpm disks in RAID 0, Windows XP professional, Postgres 8.1.3 and Postgis 1.1.4 configured as "out of the box", no extra tweaking on Postgres memory settings.

Here are the results with 100.000 reference features, and 4000 revisions modifying each 30 records (thus, 120.000 more records in the database), 220.000 records total::

	Reference data: 
	
	Running: select * from testDataRef
	Elapsed: 1.157 s, returned records:100000
	
	Running: select * from testData where expired = 9223372036854775807
	Elapsed: 1.843 s, returned records:100000
	
	Running: select * from testData where revision <= 0 and expired > 0
	Elapsed: 1.704 s, returned records:100000
	
	Running: select * from testData where revision <= 2000 and expired > 2000
	Elapsed: 1.796 s, returned records:100000
	
	Running: select * from testData where revision <= 3999 and expired > 3999
	Elapsed: 1.844 s, returned records:100000
	
	Running: select * from testDataRef where geom && GeometryFromText('POLYGON((0 0, 80 0, 80 80, 0 80, 0 0))', 4326)
	Elapsed: 0.125 s, returned records:9975
	
	Running: select * from testData where expired = 9223372036854775807 and geom && GeometryFromText('POLYGON((0 0, 80 0, 80 80, 0 80, 0 0))', 4326)
	Elapsed: 0.203 s, returned records:9975
	
	Running: select * from testData where revision <= 3999 and expired > 3999 and geom && GeometryFromText('POLYGON((0 0, 80 0, 80 80, 0 80, 0 0))', 4326)
	Elapsed: 0.235 s, returned records:9975

And here are the results with 100.000 reference features, and 10000 revisions modifying each 30 records (thus, 300.000 more records in the database), 400.000 records total::

	Running: select * from testDataRef
	Elapsed: 1.187 s, returned records:100000
	
	Running: select * from testData where expired = 9223372036854775807
	Elapsed: 1.875 s, returned records:100000
	
	Running: select * from testData where revision <= 0 and expired > 0
	Elapsed: 1.688 s, returned records:100000
	
	Running: select * from testData where revision <= 2000 and expired > 2000
	Elapsed: 1.765 s, returned records:100000
	
	Running: select * from testData where revision <= 9999 and expired > 9999
	Elapsed: 1.891 s, returned records:100000
	
	Running: select * from testDataRef where geom && GeometryFromText('POLYGON((0 0, 80 0, 80 80, 0 80, 0 0))', 4326)
	Elapsed: 0.125 s, returned records:9981
	
	Running: select * from testData where expired = 9223372036854775807 and geom && GeometryFromText('POLYGON((0 0, 80 0, 80 80, 0 80, 0 0))', 4326)
	Elapsed: 0.219 s, returned records:9981
	
	Running: select * from testData where revision <= 9999 and expired > 9999 and geom && GeometryFromText('POLYGON((0 0, 80 0, 80 80, 0 80, 0 0))', 4326)
	Elapsed: 0.234 s, returned records:9981

As you can see, despite the second run has to deal with twice the number of records in the versioned table, timings are the same.

You may wonder where is the magic. Well, the magic is good indexes and the Posgtres 8.0 onwards newly aquired ability to inspect multiple indexes during a single query, and do bitmap merging before accessing the actual data (this is used by the spatial queries). This is important, results won't be

The table creation queries are here::

	create table testData (id bigint, txt varchar(256), revision bigint, expired bigint not null, primary key (revision, id))
	select AddGeometryColumn('testdata', 'geom', 4326, 'LINESTRING', 2)
	create index testDataGeomIdx on testData using gist (geom gist_geometry_ops)
	create index testDataRevIdx on testData (expired, id)
	create table testDataRef (id bigint, txt varchar(256), primary key (id))
	select AddGeometryColumn('testdataref', 'geom', 4326, 'LINESTRING', 2)
	create index testDataRefGeomIdx on testDataRef using gist (geom gist_geometry_ops)

Observe the primary key order, which allows queries needing only revision to use the primary key as an index, and the other index, that allows the same for expired (id is there again to help queries that have to expire a certain record).

If you want to reproduce the test on your PC, the source of the benchmark is :download:`attached <images/versioningPerf.zip>`.