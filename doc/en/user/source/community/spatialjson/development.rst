.. _spatialjson_development:

Development Status
==================

The SpatialJSON format is still a playground for implementing several optimizations to transfer
even huge amounts of spatial data from the server to the client efficiently:

#. **Opt. 1: Removing redundant schema information**, see :doc:`topic <schema>`
#. **Opt. 2: Removing redundant attribute values (e. g. shared string table)**, see :doc:`topic <attributes>`
#. Opt. 3: Handling sparse rows (most values are NULL) more efficiently
#. Opt. 4: Reducing space required for geometries (e. g. differential coordinates)

Bold items have already been implemented.

The shown optimizations are ordered from *simple to implement* to *hard to implement* (not *really*
hard, however). That's also the intended order of implementation. Although some
optimizations are optional, all optimizations could be in effect at the same time. Then, each
optimization contributes his part to lower the space required for encoding a certain set of
features.

In some cases, however, it may be useful to specify which optimizations shall be used for a
request. Several techniques are available to give a client the ability to specify the set of
SpatialJSON optimizations it is able or willing to use (e. g. parameter ``format_options``,
additional ``outputFormat`` parameters). It's still not clear how this will be implemented and how
fine grained that will be.
