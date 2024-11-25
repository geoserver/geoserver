.. _security_sandbox:

Filesystem sandboxing
=====================

GeoServer administrators can usually explore the full file system of the server where GeoServer
is running into, with the same privileges as the user running the servlet container.

This can be limited by setting up a sandbox, which will restrict the access to the file system
to a specific directory tree. The sandbox can be set up at two levels:

* **System sandbox**: the GeoServer administrator is sandboxed into a specific directory, and won't be
  able to access files outside of it, nor change the sandbox configuration.
* **Regular sandbox**: the GeoServer administrator can still access the full file system, but can set up
  a sandbox for each workspace, where the workspace administrators will be sandboxed into.

.. warning:: The importer extension allows upload of data and is currently unable to respect the file system sandbox,
    it uses a configurable location inside the data directory instead. Store creation will fail if the importer
    is used and the sandbox is set.

Setting up a system sandbox
---------------------------

The system sandbox is configured by setting the ``GEOSERVER_FILESYSTEM_SANDBOX`` variable to the
directory where the GeoServer administrator should be sandboxed into.
The variable can be provided as a Java system variable, as a servlet context parameter, or as an
environment variable, please consult the :ref:`application_properties` section for more details.

When the system sandbox is set:

* The GeoServer administrator will be sandboxed into the configured directory,
  and won't be able to access files outside of it, nor change the sandbox configuration.
* The GeoServer workspace administrators will be sandboxed into ``<sandbox>/<workspace>``, where
  ``<workspace>`` is the name of any workspace they can access.

The system sandbox is best suited in hosting environments, where the GeoServer administrator and the
operating system administrator are different people, and the GeoServer administrator should not be
able to access the full file system.

Setting up a regular sandbox
----------------------------

The regular sandbox can be configured by GeoServer full administrators in the user interface,
from the :guilabel:`Security` -> :guilabel:`Data` page, or by adding the following entry in the
``layers.properties`` file:

.. code-block:: properties

   # Set the sandbox for the workspace
   filesystemSandbox=/path/to/sandbox

When the regular sandbox is set:

* The GeoServer administrator will still be able to access the full file system,
  as well as change the sandbox configuration if they so desire.
* The GeoServer workspace administrators will be sandboxed into ``<sandbox>/<workspace>``, where
  ``<workspace>`` is the name of any workspace they can access.

The regular sandbox is best suited in multi-tenant environments where the main GeoServer administrator
also has access to the server operating system, while each tenant is modelled as a workspace
administrator and should be able to manage its own data, but not access the data of other tenants.
