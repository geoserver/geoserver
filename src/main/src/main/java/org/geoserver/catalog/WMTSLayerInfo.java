/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;

import org.geotools.data.ows.Layer;
import org.opengis.util.ProgressListener;

public interface WMTSLayerInfo extends ResourceInfo {

    public WMTSStoreInfo getStore();

    /**
     * Returns the raw WMTS layer associated to this resource
     * 
     *
     */
    public Layer getWMTSLayer(ProgressListener listener) throws IOException;
   
}
