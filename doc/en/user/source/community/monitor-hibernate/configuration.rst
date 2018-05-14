.. _monitor_hibernate_configuration:

Hibernate storage Configuration
===============================

Many aspects of the monitor extension are configurable. The configuration files
are stored in the data directory under the ``monitoring`` directory::

  <data_directory>
      monitoring/
          db.properties
          hibernate.properties


In particular:
* **db.properties** - Database configuration when using database persistence.
* **hibernate.properties** - Hibernate configuration when using database persistence.

Monitor Storage
---------------

How request data is persisted is configurable via the ``storage`` property defined in the 
``monitor.properties`` file. The following values are supported for the ``storage`` property:

* **memory** - Request data is to be persisted in memory alone.
* **hibernate** - Request data is to be persisted in a relational database via Hibernate.

The default value is ``memory``, in order to use hibernate the ``storage`` configuration needs
to be switced to ``hibernate``.

