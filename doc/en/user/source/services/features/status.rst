OGC API - Features Implementation status
----------------------------------------

.. list-table::
   :widths: 30, 20, 50
   :header-rows: 1

   * - `OGC API - Features <https://github.com/opengeospatial/ogcapi-features>`__
     - Version
     - Implementation status
   * - Part 1: Core
     - `1.0.1 <https://docs.ogc.org/is/17-069r4/17-069r4.html>`__
     - Passes compliance tests
   * - Part 2: Coordinate Systems by Reference
     - `1.0.1 <https://docs.ogc.org/is/18-058r1/18-058r1.html>`__
     - Passes compliance tests
   * - Part 3: Filtering
     - `1.0.0 <https://docs.ogc.org/is/19-079r2/19-079r2.html>`__
     - Implemented an earlier draft, being updated to final (no CITE tests yet)
   * - Common Query Language (CQL2)
     - `1.0.0 <https://docs.ogc.org/is/21-065r2/21-065r2.html>`__
     - Implemented an earlier draft, being updated to final (no CITE tests yet)
   * - Part 4: Create, Replace, Update and Delete
     - `1.0.0 <https://docs.ogc.org/DRAFTS/20-002r1.html>`__
     - Not implemented (volunteers/sponsoring wanted)
   * - Part 5: search
     - `Proposal DRAFT <https://github.com/opengeospatial/ogcapi-features/tree/master/proposals/search>`__
     - A search endpoint for complex queries is implemented at the single collection level (POST to immediately get a response, no support for stored queries).
   * - Part 6 - Schemas
     - `Proposal DRAFT <https://github.com/opengeospatial/ogcapi-features/tree/master/proposals/search>`__
     - Not implemented (volunteers/sponsoring wanted)
   * - Part n: Query by IDs
     - `Proposal DRAFT <https://github.com/opengeospatial/ogcapi-features/tree/master/proposals/query-by-ids>`__
     - Proposal implemented, but syntax and semantic is subject to change in a future release. Thus said, usage should be carefully considered.
   * - Sorting
     - `DRAFT in github <https://github.com/opengeospatial/ogcapi-features/tree/master/extensions/sorting/standard>`__
     - Partial implementation borrowed by OGC API Records, using the sortby parameter. Sortables are not exposed.


