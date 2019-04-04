.. _ysld_vendor:

GeoServer Specific Extensions
=============================

GeoWebCache Integration
-----------------------

When defining rules in terms of zoom levels, you can use the zoom level from a gridset defined in the integrated GeoWebCache instance.

For instance, if your GWC had a gridset named ``CanadaLCCQuad`` and you wanted a style rule to apply to levels 0-2 of that gridset you could use the following::
  
  grid:
    name: CanadaLCCQuad
  rules:
  - zoom: [0,2]
    point:
      ...
