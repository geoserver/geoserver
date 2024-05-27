.. _community_rat:

Raster Attribute Table support
==============================

A GDAL Raster Attribute Table (RAT) is a data structure associated with a raster dataset, 
providing a way to associate attribute information for individual pixel values within the raster. 
Essentially, it acts as a tabular data structure that links each cell value in the raster to one or more attributes.

The RAT consists of rows and columns, where each row corresponds to a unique cell value in the raster, 
and each column represents a different attribute or property of those cells. 
These attributes can be numerical, categorical, text-based 
and may include information like land cover types, elevation values, or any other relevant data, but
may also contain color information that can be used to render the raster in a more visually appealing way.

The RAT is stored in a separate file from the raster data itself, and is typically stored in a ".aux.xml" file,
as part of a PAMDataset. Each of the bands in the PAM can contain a separate RAT, allowing
for different attributes to be associated with each band.

One example of RAT usage is the NOAA Bluetopo dataset, which contains 3 floating points bands:

* The first band contains bathymetry data
* The second band contains a measure of uncertainty
* The third band, dubbed "contributor", links to a RAT that contains various information about the source of the data, such as the name of the source institution, the source license, date of survey, and more.

In this section:

.. toctree::
   :maxdepth: 1

   installing
   using
