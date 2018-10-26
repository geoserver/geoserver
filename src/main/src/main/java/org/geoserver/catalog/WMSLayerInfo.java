/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import org.geotools.ows.wms.Layer;
import org.opengis.util.ProgressListener;

public interface WMSLayerInfo extends ResourceInfo {

    public WMSStoreInfo getStore();

    /** Returns the raw WMS layer associated to this resource */
    public Layer getWMSLayer(ProgressListener listener) throws IOException;

    /** Return the DataURLs associated with this */
}
