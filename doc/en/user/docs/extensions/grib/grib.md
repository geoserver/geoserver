# GRIB

## Adding a GRIB data store

To add a GRIB data store the user must go to **Stores --> Add New Store --> GRIB**.

![](gribcreate.png)
*GRIB in the list of raster data stores*

## Configuring a GRIB data store

![](gribconfigure.png)
*Configuring a GRIB data store*

|                    |                 |
|--------------------|-----------------|
| **Option**         | **Description** |
| `Workspace`        |                 |
| `Data Source Name` |                 |
| `Description`      |                 |
| `Enabled`          |                 |
| `URL`              |                 |

## Relationship with NetCDF

!!! note

    Note that internally the GRIB extension uses the NetCDF reader, which supports also GRIB data. See also the [NetCDF](../netcdf/netcdf.md) documentation page for further information.

## Current limitations

-   Input coverages/slices should share the same bounding box (lon/lat coordinates are the same for the whole ND cube)
