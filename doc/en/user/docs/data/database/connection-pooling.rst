.. _connection_pooling:

Database Connection Pooling
===========================

When serving data from a spatial database *connection pooling* is an important aspect of achieving good performance. When GeoServer serves a request that involves loading data from a database table, a connection must first be established with the database. This connection comes with a cost as it takes time to set up such a connection.

The purpose served by a connection pool is to maintain connection to an underlying database between requests. The benefit of which is that connection setup only need to occur once on the first request. Subsequent requests use existing connections and achieve a performance benefit as a result.

Whenever a data store backed by a database is added to GeoServer an internal connection pool is created. This connection pool is configurable.

Connection pool options
-----------------------

.. list-table::
   :widths: 20 80

   * - max connections 
     - The maximum number of connections the pool can hold. When the maximum number of connections is exceeded, additional requests that require a database connection will be halted until a connection from the pool becomes available. The maximum number of connections limits the number of concurrent requests that can be made against the database.
   * - min connections
     - The minimum number of connections the pool will hold. This number of connections is held even when there are no active requests. When this number of connections is exceeded due to serving requests additional connections are opened until the pool reaches its maximum size (described above).
   * - validate connections
     - Flag indicating whether connections from the pool should be validated before they are used. A connection in the pool can become invalid for a number of reasons including network breakdown, database server timeout, etc..
       The benefit of setting this flag is that an invalid connection will never be used which can prevent client errors. The downside of setting the flag is that a performance penalty is paid in order to validate connections.
   * - fetch size
     - The number of records read from the database in each network exchange. If set too low (<50) network latency will affect negatively performance, if set too high it might consume a significant portion of GeoServer memory and push it towards an ``Out Of Memory`` error. Defaults to 1000.
   * - connection timeout
     - Time, in seconds, the connection pool will wait before giving up its attempt to get a new connection from the database. Defaults to 20 seconds. 
   * - Test while idle
     - Periodically test if the connections are still valid also while idle in the pool. Sometimes performing a test query using an idle connection can make the datastore unavailable for a while. Often the cause of this troublesome behaviour is related to a network firewall placed between GeoServer and the Database that force the closing of the underlying idle TCP connections.
   * - Evictor run periodicity
     - Number of seconds between idle object evictor runs.
   * - Max connection idle time
     - Number of seconds a connection needs to stay idle before the evictor starts to consider closing it.
   * - Evictor tests per run
     - Number of connections checked by the idle connection evictor for each of its runs.
