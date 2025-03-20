.. _monitor_micrometer_usage:

Usage of the Monitor Micrometer Extension
=========================================

After enabling the Monitor Micrometer extension in the ``monitor.properties`` file by setting ``micrometer.enable`` to ``true``,
HTTP requests made to GeoServer will be processed by the extension and recorded in Micrometer’s internal registry.

The collected metrics are exposed through `geoserver/rest/monitor/requests/metrics`,
which provides an HTTP endpoint compatible with Prometheus’s scraper.

.. note:: The metrics page will initially be empty if no requests have been made to GeoServer. 
   To verify whether requests have been recorded by the monitoring system, 
   you can `query the Monitor request data storage via HTTP <https://docs.geoserver.org/latest/en/user/extensions/monitoring/query.html#all-requests-as-html>`_, 
   accessible at `geoserver/rest/monitor/requests.html`.

Metrics Collected
-----------------

The following is a list of metrics collected by the Monitor Micrometer extension, formatted for compatibility with Prometheus as displayed on the `metrics` page.
For further details, refer to the `Monitoring Extension documentation <https://docs.geoserver.org/latest/en/user/extensions/monitoring/reference.html#data-reference>`_.

* ``requests_total_seconds``

| The total time spent handling the request, measured in seconds.
| Available metrics types:

    * **requests_total_seconds (summary)**: tracks the total duration of requests (``sum``) and their number (``count``).
    * **requests_total_seconds_max (gauge)**: tracks the highest recorded request time at a given moment (``max``).

* ``requests_response_length_bytes``

| The total number of bytes comprising the response to the request.
| Available metrics types:

    * **requests_response_length_bytes (summary)**: tracks the length of responses (``sum``) and their number (``count``).
    * **requests_response_length_bytes_max (gauge)**: tracks the highest recorded response length at a given moment (``max``).

* ``requests_processing_seconds``

| Sum of the rendering times for resources in seconds.
| Available metrics types:

    * **requests_processing_seconds (summary)**: tracks the total duration of request processing (``sum``) and the number of requests (``count``).
    * **requests_processing_seconds_max (gauge)**: tracks the highest recorded request processing time at a given moment (``max``).

* ``requests_labelling_processing_seconds``

| Processing time in seconds for the labels of all requested resources.
| Available metrics types:

    * **requests_labelling_processing_seconds (summary)**: tracks the total duration of labeling processing (``sum``) and the number of requests (``count``).
    * **requests_labelling_processing_seconds_max (gauge)**: tracks the highest recorded labelling processing time at a given moment (``max``).

* ``requests_host_total``

| This metric is only available when ``micrometer.metric.remote_host.enabled`` is set to true.
| Tracks the total number of requests made by different remote callers.
| Available metrics types:

    * **requests_host_total (counter)**: tracks the total number of requests for each unique combination of remote address, remote host, and remote user (``total``).

Tags
----

The following tags are associated with the metrics ``requests_total_seconds``, ``requests_response_length_bytes``, ``requests_processing_seconds``, and ``requests_labelling_processing_seconds``:

* ``errorMessage``: The error message, if any (empty if no error occurred).
* ``httpMethod``: The HTTP method used (e.g., `GET`, `POST`).
* ``operation``: The requested OWS operation (e.g., `GetMap`).
* ``owsVersion``: The OWS service version (e.g., `1.3.0` for WMS).
* ``resources``: The requested resource(s) (e.g., `sf:roads`).
* ``responseContentType``: The MIME type of the response (e.g., `image/jpeg`).
* ``responseStatus``: The HTTP response status code (e.g., `200` for success).
* ``service``: The OGC service being requested (e.g., `WMS`).
* ``status``: The final status of the request processing (e.g., `FINISHED`).

For the remote host information counter metric `requests_host_total`, the associated tags also include the following:

* ``remoteAddr``: The IP address of the client making the request.
* ``remoteHost``: The resolved hostname of the client (if available).
* ``remoteUser``: The authenticated user making the request (e.g., `anonymous` if not authenticated).