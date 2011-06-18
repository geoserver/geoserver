/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;

import org.geotools.data.ows.Layer;
import org.opengis.util.ProgressListener;

public interface WMSLayerInfo extends ResourceInfo {

    public WMSStoreInfo getStore();

    /**
     * Returns the raw WMS layer associated to this resource
     * 
     * @return
     */
    public Layer getWMSLayer(ProgressListener listener) throws IOException;

}
