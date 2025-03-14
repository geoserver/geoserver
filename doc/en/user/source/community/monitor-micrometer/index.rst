.. _monitor_micrometer_extension:

Monitoring with Micrometer support
==================================

The Monitor Micrometer extension provides a specialized visualization of the requests made to a GeoServer instance,
integrating a `Micrometer <https://micrometer.io>`_ registry to expose a `Prometheus <https://prometheus.io>`_-compatible metrics endpoint. 
This allows external Prometheus instances to scrape and collect usage metrics for GeoServer, enabling better insight into request patterns and system performance.

This extension provides fine-grained control over how request data is incorporated into Micrometer’s registry.  
Each metric measures a specific aspect of a request, such as:

- **Timers**, which track request durations (e.g., how long a WFS request takes to complete).
- **Counters**, which measure occurrences (e.g., the number of requests made for a specific WMS layer).

For each request, the following metrics are recorded:

* **Request total time**
* **Response length**
* **Resource processing times**
* **Label processing time**
* **Remote calling host information** (optional)

.. toctree::
   :maxdepth: 2

   installation/
   configuration/
   usage/
