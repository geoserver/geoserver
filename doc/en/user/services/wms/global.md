# Global variables affecting WMS

This document details the set of global variables that can affect WMS behaviour. Each global variable can be set as an environment variable, as a servlet context variable, or as a Java system property, just like the well known `GEOSERVER_DATA_DIR` setting. Refer to [data directory settings](../../datadirectory/setting.md) for details on how a global variable can be specified.

## MAX_FILTER_RULES

A integer number (defaults to 20).

When drawing a style containing multiple active rules, the renderer combines the filters of the rules in OR and adds them to the standard bounding box filter. This behaviour is active until the maximum number of filter rules is reached; beyond that, the rule filters are no longer added to avoid huge queries. By default up to 20 rules are combined; past 20 rules only the bounding box filter is used.

Turning it off (setting it to `0`) can be useful if the styles are mostly classifications, and detrimental if the rule filters are actually filtering a good amount of data out.

## OPTIMIZE_LINE_WIDTH

Can be `true` or `false` (defaults to `false`).

When `true`, any stroke whose width is less than 1.5 pixels gets slimmed down to "zero", which is actually not zero but a very thin line. That was the behaviour GeoServer used to default to before the 2.0 series.

When `false`, the stroke width is not modified and it is possible to specify widths less than one pixel. This is the default behaviour starting from the 2.0.0 release.

## ENABLE_JSONP

Can be `true` or `false` (defaults to `false`).

When `true`, the JSONP (`text/javascript`) output format is enabled.
