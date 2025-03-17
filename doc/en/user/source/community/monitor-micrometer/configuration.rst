.. _monitor_micrometer_configuration:

Monitor Micrometer Configuration
================================

Many aspects of the monitor extension are configurable. The configuration files
are stored in the data directory under the ``monitoring`` directory::

  <data_directory>
      monitoring/
          monitor.properties

In particular:

* **micrometer.enabled** - Enables or disables the Monitor Micrometer extension.
* **micrometer.metric.reset_count** - Sets the maximum number of requests to track before resetting the Micrometer registry.
* **micrometer.metric.remote_host.enabled** - Enables or disables tracking of remote host request information.

Micrometer Registry Reset Count
-------------------------------

To avoid tracking individual requests separately, requests are grouped by attribute values, such as response status or OGC protocol. 
Each attribute is assigned a name, which serves as a Micrometer Tag. Requests sharing the same tag values are aggregated into a single metric group, 
ensuring efficient and meaningful data collection.

However, a challenge with this approach is that the number of metric groups can grow significantly depending on how requests are categorized. 
Since metrics are grouped by attribute values, the number of unique groups can become very large in high-traffic environments, potentially impacting performance and memory usage.

To mitigate this, the Monitor Micrometer extension provides the ``micrometer.metric.reset_count`` configuration option to enforce a maximum number of tracked requests. 
Once this limit is reached, the Micrometer registry resets, clearing the existing metrics and starting fresh. 
This prevents the system from accumulating an unbounded number of metrics over time.

The default value for this configuration option is ``100``, aligning with the limit set for the `Memory Storage <https://docs.geoserver.org/latest/en/user/extensions/monitoring/configuration.html#memory-storage>`_ of request data.
Like request data storage, the Micrometer registry is also volatile, meaning it resets whenever the GeoServer instance is restarted.

Remote Host Metrics
-------------------

Since the Micrometer registry could grow significantly in metric cardinality due to the variability of remote host request data, 
the ``micrometer.metric.remote_host.enabled`` configuration flag allows users to enable HTTP “remote” information metrics only when necessary, reducing the number of unique metric groups.

Enabling this metric can be useful, for example, when there is a specific need to monitor the composition of hosts making HTTP requests to GeoServer.

By default, remote host request metrics are disabled.
