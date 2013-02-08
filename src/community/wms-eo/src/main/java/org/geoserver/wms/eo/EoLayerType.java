/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2013, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wms.eo;


/**
 * Enum of WMS-EO layer types.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public enum EoLayerType {

    EO_PRODUCT, COVERAGE_OUTLINE, BAND_COVERAGE, GEOPHYSICAL_PARAMETER, BITMASK;

    /**
     * Key used in LayerInfo metadata to store EO Layer type
     */
    public static final String KEY = "WMSEO-LAYER";
    
}