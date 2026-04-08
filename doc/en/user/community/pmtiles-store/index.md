# PMTiles DataStore

This module provides support for [Protomaps PMTiles](https://protomaps.com/docs/pmtiles) as a vector data source in GeoServer.

PMTiles is a cloud-optimized single-file archive format for tiled data. It enables efficient access to vector tiles from:

- Local file systems
- HTTP/HTTPS servers with range request support
- Cloud object storage (Amazon S3, Azure Blob Storage, Google Cloud Storage)

The format uses a hierarchical directory structure that allows random access to individual tiles without reading the entire file, making it ideal for serving large datasets directly from static storage without requiring a dedicated tile server.

<div class="grid cards" markdown>

- [Installing the PMTiles DataStore Extension](installing.md)
- [Using PMTiles DataStores](usage.md)

</div>

## Features

- Read Mapbox Vector Tiles from PMTiles archives
- Support for multiple data sources (file, HTTP, S3, Azure, GCS)
- Memory caching with configurable block alignment for optimal performance
- Cloud-native workflows with HTTP range requests
- Authentication support for HTTP, S3, Azure, and GCS
