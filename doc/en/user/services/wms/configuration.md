# WMS configuration

## Layer Groups

A Layer Group is a group of layers that can be referred to by one layer name. For example, if you put three layers (call them `layer_A`, `layer_B`, and `layer_C`) under one Layer Group layer, then when a user makes a WMS `GetMap` request for that group name, they will get a map of those three layers.

For information on configuring Layer Groups in the Web Administration Interface see [Layer Groups](../../data/webadmin/layergroups.md).

## Request limits

The request limit options allow the administrator to limit the resources consumed by each WMS `GetMap` request.

The following table shows the option names, a description, and the minimum GeoServer version at which the option is available.

| Option | Description | Version |
| --- | --- | --- |
| **Max rendering memory (KB)** | Sets the maximum amount of memory a single GetMap request is allowed to use (in kilobytes). The limit is checked before request execution by estimating how much memory would be required to produce the output result. If the estimated memory size is below the limit, the request is executed; otherwise it is cancelled. | 1.7.5 |
| **Max rendering time (s)** | Sets the maximum number of seconds GeoServer will spend processing a request. This time limits the "blind processing" portion of the request, i.e. reading data and computing the output result. The execution time does not include the time taken to write results back to the client. | 1.7.5 |
| **Max rendering errors (count)** | Sets the maximum number of rendering errors tolerated by a GetMap request. By default GetMap makes a best-effort attempt to serve the result, ignoring invalid features, reprojection errors and the like. Limiting the number of errors can help identify issues and conserve CPU cycles. | 1.7.5 |
| **Max number of dimension values** | Sets the maximum number of dimension (time, elevation, custom) values that a client can request in a GetMap/GetFeatureInfo request. The work is usually proportional to the number of values, which are held in memory. | 2.14.0 |

The default value of each limit is `0`, which means the limit is not applied.

When any of the request limits is exceeded, the GetMap operation is cancelled and a `ServiceException` is returned to the client.

When setting these limits, consider peak conditions. For example, under normal circumstances a GetMap request may take less than a second. Under high load it may take longer, but it is usually not desirable to allow it to run for 30 minutes.

## LayerGroup capabilities settings

| Option | Description |
| --- | --- |
| Default LayerGroup Style In GetCapabilities | Enable/disable the encoding of the default LayerGroup style in GetCapabilities responses for LayerGroup with mode Named Tree, Container Tree, or Earth Observation Tree. Single and Opaque groups are not affected by the option and will always show the default style. By default the option is set to enabled. |
