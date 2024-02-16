# GetFeatureInfo Templates {: #tutorials_getfeatureinfo }

This tutorial describes how to use GeoServer to create custom GetFeatureInfo responses.

## Introduction

GetFeatureInfo is a WMS standard call that allows you to retrieve information about features and coverages displayed in a map. The map can be composed of various layers, and GetFeatureInfo can be instructed to return multiple feature descriptions, which may be of different types. GetFeatureInfo can generate output in various formats: GML2, plain text, GeoJSON and HTML. Templating is concerned with the HTML and GeoJSON ones. While raster layers have a few more specific customization capabilities.

-   [HTML output format](html.md)
-   [GeoJSON output format](geojson.md)
-   [Raster GetFeatureInfo Response Customization](raster.md)
