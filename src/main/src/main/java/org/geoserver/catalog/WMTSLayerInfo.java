/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import org.geotools.ows.wmts.model.WMTSLayer;
import org.opengis.util.ProgressListener;

public interface WMTSLayerInfo extends ResourceInfo {

    public WMTSStoreInfo getStore();

    /** Returns the raw WMTS layer associated to this resource */
    public WMTSLayer getWMTSLayer(ProgressListener listener) throws IOException;
}
