/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sld;

import java.util.List;
import org.geoserver.wms.WMSRequest;

public class GetStylesRequest extends WMSRequest {

    List<String> layers;

    String sldVer;

    public GetStylesRequest() {
        super("GetStyles");
    }

    public String getSldVer() {
        return sldVer;
    }

    public void setSldVer(String sldVer) {
        this.sldVer = sldVer;
    }

    public List<String> getLayers() {
        return layers;
    }

    public void setLayers(List<String> layers) {
        this.layers = layers;
    }
}
