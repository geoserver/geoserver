.. _datadir_catalog_loader:

DataDirectory Catalog Loader
============================

Overview
--------

The **Catalog DataDirectory Loader** module is a GeoServer community extension designed to significantly
improve the startup time for GeoServer instances with **large catalogs**, which may include thousands of
workspaces, stores, and layers. This module optimizes the loading and parsing of catalog configuration
files, making it particularly relevant for environments where the data directory resides on a **network filesystem (e.g., NFS)**.

Network filesystems are notoriously inefficient at handling a large number of small file reads, such
as those required to load XML files for GeoServer's catalog. This module addresses these inefficiencies
by employing a **single-pass directory traversal** and **parallelized parsing**, leading to substantial
performance gains.

Features
--------

- **Parallelized Loading**:
  - File I/O operations and XML parsing are executed concurrently to maximize efficiency.
- **Single Directory Traversal**:
  - Catalog (e.g., workspaces, layers, stores) and configuration (e.g., services, settings) files are processed in a single pass over the `workspaces` directory.
- **Deferred Password Decryption**:
  - Password decryption is deferred to avoid potential thread deadlocks during startup.

Why Use This Module?
---------------------

GeoServer catalogs with thousands of workspaces, stores, and layers are represented by numerous small
XML files in the data directory. This can cause significant startup delays, especially when the catalog
is located on a **network filesystem (NFS)** or other storage with high latency for small file operations.

The **Catalog DataDirectory Loader** module provides:

- **Improved Startup Times**:
  - For catalogs stored on a **local disk**, the performance improvement can exceed **30%**, significantly reducing initialization times.
  - On **network filesystems (e.g., NFS)**, the improvement can reach **one order of magnitude**, as the module optimizes I/O operations that are inefficient on such storage systems.
  - In a real-world case involving a catalog with approximately **80,000 layers**, **hundreds of workspaces**, and **workspace-specific service configurations**, the startup time was reduced from **40 minutes to just 4 minutes** using this module.

- **Optimized for NFS**:
  - A single directory traversal minimizes the number of file operations, addressing inefficiencies in network filesystems.
- **Scalable Solution for Large Catalogs**:
  - Handles catalogs with thousands of configurations without compromising performance.

How It Works
------------

Parallelized Catalog Loading
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The module utilizes multiple threads to handle:

1. **File I/O Operations**:
   - Efficiently read files from the `workspaces` directory and other catalog-related paths.
2. **XML Parsing**:
   - Simultaneously parse workspace, store, layer, and service configurations.

This parallel approach ensures that both I/O and parsing occur concurrently, reducing startup delays.

Single-Pass Directory Traversal
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Traditional GeoServer catalog loading requires multiple passes over the `workspaces` directory to load catalog (workspaces, layers) and configuration (settings, services) files separately. This module combines these operations into a **single directory traversal**, drastically reducing redundant I/O operations.

This optimization is particularly effective for **large catalogs on NFS**, where each small file operation introduces significant overhead.

Deferred Password Decryption
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To prevent thread deadlocks during catalog initialization, the module defers the decryption of sensitive information such as passwords until after the catalog is fully loaded. This includes:

- `DataStoreInfo.connectionParameters`
- `HTTPStoreInfo.password`

This prevents the **XStream deserializer** from invoking `GeoServerExtensions` in a way that causes
threads to block due to Spring deadlocks. Such deadlocks can occur when `GeoServerExtensions` attempts
to resolve a bean that has not yet been fully initialized, resulting in a smoother and more reliable startup process.

Installation
------------

1. Download the `gs-datadir-catalog-loader` extension JAR file from the official GeoServer community module repository.
2. Copy the JAR file to the `WEB-INF/lib` directory of your GeoServer installation.
3. Restart GeoServer to activate the module.

Configuration
-------------

The **DataDirectory Catalog Loader** module automatically replaces the default GeoServer data
directory loader upon installation. By default, it becomes the active loader and optimizes the
loading and parsing of the catalog.

If needed, the module can be disabled using the `DATADIR_LOADER_ENABLED` environment variable.
This allows users to revert to the default data directory loader without uninstalling the module.

Enabling or Disabling the Module
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To disable the **DataDirectory Catalog Loader** and revert to the default GeoServer data directory
loader, set the `DATADIR_LOADER_ENABLED` environment variable to `false` before starting GeoServer:

.. code-block:: bash

   export DATADIR_LOADER_ENABLED=false
   bin/startup.sh

Parallelism Level
^^^^^^^^^^^^^^^^^

The module uses a parallel approach to load and parse catalog configuration files. By default, the number
of threads is determined by a heuristic that resolves to the lesser of:

- **16 threads**, or
- The number of processors available as reported by `Runtime#availableProcessors()`.

Customizing Parallelism Level
"""""""""""""""""""""""""""""

To override the default parallelism level, set the `DATADIR_LOAD_PARALLELISM` environment variable to the
desired number of threads before starting GeoServer:

.. code-block:: bash

   export DATADIR_LOAD_PARALLELISM=4
   bin/startup.sh

Limitations
-----------

- This module focuses on optimizing file loading and parsing but does not modify the structure of the GeoServer catalog.
- It is most effective for large catalogs and environments where startup performance is critical.
- Shared drives with extremely high latency may still pose challenges.

Community Module Disclaimer
---------------------------

This module is part of the GeoServer community extensions and is not officially supported. Use it at your own risk and test thoroughly before deploying in production.
