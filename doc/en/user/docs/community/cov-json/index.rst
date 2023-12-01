.. _cov_json:

CoverageJSON output format
==========================

`CoverageJSON <https://covjson.org/>`_ is a format for encoding geotemporal coverage data like grids and time series. For example, it can be as output format for a WCS2.0 getCoverage requesting a TimeSeries on a specific coordinate. As per the specification, the format to be specified to get back a cov-json output is *application/prs.coverage+json*.

Installation
------------

As a community module, the cov-json package needs to be downloaded from the `nightly builds <https://build.geoserver.org/geoserver/>`_,
picking the community folder of the corresponding GeoServer series (e.g. if working on the GeoServer main development branch nightly
builds, pick the zip file form ``main/community-latest``).

To install the module, unpack the zip file contents into GeoServer own ``WEB-INF/lib`` directory and restart GeoServer.

Example: WCS 2.0 - TimeSeries
-----------------------------

Let *test:timeseries* be a sample layer related to a coverage with time dimensions enabled. Suppose we want to get the coverage values for a specific time period on a specific lat/lon coordinate. That will be a slicing on lat/lon coordinate and trimming on time dimension.

A getCoverage request can be posted with a similar content to http://server:port/geoserver/wcs:

.. code-block:: xml

  <wcs:GetCoverage service="WCS" version="2.0.1"
  xmlns:wcs="http://www.opengis.net/wcs/2.0"
  xmlns:gml="http://www.opengis.net/gml/3.2" 
  xmlns:crs="http://www.opengis.net/spec/WCS_service-extension_crs/1.0"
  xmlns:rsub="http://www.opengis.net/spec/wcs_service-extension_range-subsetting/1.0"
  xmlns:scal="http://www.opengis.net/spec/WCS_service-extension_scaling/1.0/"
  xmlns:wcsgeotiff="http://www.opengis.net/wcs/geotiff/1.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.opengis.net/wcs/2.0 http://schemas.opengis.net/wcs/2.0/wcsAll.xsd">

    <wcs:CoverageId>test__timeseries</wcs:CoverageId>
    <wcs:format>application/prs.coverage+json</wcs:format>
    <wcs:DimensionSlice>
      <wcs:Dimension>Long</wcs:Dimension>
      <wcs:SlicePoint>56</wcs:SlicePoint>
    </wcs:DimensionSlice>
    <wcs:DimensionSlice>
      <wcs:Dimension>Lat</wcs:Dimension>
      <wcs:SlicePoint>0</wcs:SlicePoint>
    </wcs:DimensionSlice>
    <wcs:DimensionTrim>
      <wcs:Dimension>Time</wcs:Dimension>
      <wcs:TrimLow>2014-01-01T00:00:00.000Z</wcs:TrimLow>
      <wcs:TrimHigh>2017-01-01T00:00:00.000Z</wcs:TrimHigh>
    </wcs:DimensionTrim>
  </wcs:GetCoverage>

The outcome will be something like this:

.. code-block:: json

  {
    "type": "Coverage",
    "domain": {
    "type": "Domain",
    "domainType": "PointSeries",
    "axes": {
      "x": {
      "values": [
        56.0
      ]
      },
      "y": {
      "values": [
        1.0112359550561785
      ]
      },
      "t": {
      "values": [
        "2014-01-01T00:00:00Z",
        "2015-01-01T00:00:00Z",
        "2016-01-01T00:00:00Z",
        "2017-01-01T00:00:00Z"
      ]
      }
    },
    "referencing": [
      {
      "coordinates": [
        "x",
        "y"
      ],
      "system": {
        "type": "GeographicCRS",
        "id": "http://www.opengis.net/def/crs/EPSG/0/4326"
      }
      },
      {
      "coordinates": [
        "t"
      ],
      "system": {
        "type": "TemporalRS",
        "calendar": "Gregorian"
      }
      }
    ]
    },
    "parameters": {
    "TIMESERIES": {
      "type": "Parameter",
      "description": {
      "en": "timeseries"
      },
      "observedProperty": {
      "label": {
        "en": "timeseries"
      }
      }
    }
    },
    "ranges": {
    "TIMESERIES": {
      "type": "NdArray",
      "dataType": "float",
      "axisNames": [
      "t",
      "y",
      "x"
      ],
      "shape": [
      4,
      1,
      1
      ],
      "values": [
      25.5,
      24.76,
      26.06,
      23.22
      ]
    }
    }
  }


  
Note the domainType = PointSeries where x,y axes have a single and the t axis has 4 times in the values. Also note the ranges property is reporting 4 values.
