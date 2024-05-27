CSW ISO Metadata Profile Queryables
===================================

Mapping
~~~~~~~
The ISO Metadata standard (see `OGC Implementation Specification 07-045 <http://www.opengeospatial.org/standards/specifications/catalog>`_) specifies a mapping of XPaths to CSW queryables. These are simple property names such as 'Title' or 'Abstract' that can be used in filters and will automatically be translated to a corresponding XPath.

In some case it might be required to have an alternative mapping of queryables (to a different, non-default XPaths). For instance when records represent services rather than data, the corresponding XPaths are different.

The ISO Metadata queryables mapping can be found in the file ``csw/MD_Metadata.queryables.properties`` inside the data directory. It follows the format of a properties file, where each value can be a comma-separated list of XPaths. When a queryable is linked to multiple XPaths, filters will include records that have a match for any of them. For the `GetDomain` request, the domains of all the XPaths will be merged. Although the intended use case is that each record only has a value for either one of the properties, the user is responsible for configuring valid mappings.

Bounding Box
~~~~~~~~~~~~
The 'BoundingBox' queryable has an additional functionality. Changing it will not only alter the queryable as such, but also how the layers are mapped to ISO metadata records. The first XPath will be the place where the bounding box of the layers are encoded in each of the records.

Multiple Mappings
~~~~~~~~~~~~~~~~~
The CSW module supports mapping each layer to multiple records (see :ref:`csw_mapping_file`). In this case one might want to have separate queryables mappings associated with these distinct mappings as well. (One could avoid this and map queryables to multiple XPaths instead as explained above, but that would not make it possible to map the bounding box to separate XPaths). The syntax of the queryables mappings file names is analogue to the regular mappings. For instance, or instance, one could have the following files in the ``csw`` directory:

* ``csw/Record.properties``
* ``csw/Record-otherRecord.properties``
* ``csw/Record.queryables.properties``
* ``csw/Record-otherRecord.queryables.properties``