/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo;


/**
 * Enum of WMS-EO layer types.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public enum EoLayerType {

    BROWSE_IMAGE, COVERAGE_OUTLINE, BAND_COVERAGE, GEOPHYSICAL_PARAMETER, BITMASK;

    /**
     * Key used in LayerInfo metadata to store EO Layer type
     */
    public static final String KEY = "WMSEO-LAYER";
    
}