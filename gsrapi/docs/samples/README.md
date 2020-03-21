# Samples

This directory contains samples of using GSR, primarily with the
ESRI JS API.

* dynamic_map_layer: adding a regular MapServer to the map
* feature_layers: layers using FeatureServer.

## Usage

These samples include a sample data directory pre-populated with everything needed to
run them. Simply point a GeoServer 2.12 instance at it and go. The demos assume port 8080.
The following flags are an example of how to do this:

`-DGEOSERVER_DATA_DIR=/Users/devon/Projects/gsr/docs/samples/gsr_demo_data_dir -Djetty.port=8080`

## CORS Issues
It is strongly recommended to enable CORS support on your server. If you have issues, particularly on requests
that have a callback request parameter, try enabling CORS support on your GeoServer.

The demos should be set up already to add localhost to the CORS whitelist.

Refer to the main readme for further CORS instructions.