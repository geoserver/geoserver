/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sld;

import org.geoserver.ows.KvpRequestReader;
import org.geoserver.wms.WMS;

public class GetStylesKvpRequestReader extends KvpRequestReader {

    WMS wms;

    public GetStylesKvpRequestReader(WMS wms) {
        super(GetStylesRequest.class);
        this.wms = wms;
    }

    @Override
    public Object createRequest() throws Exception {
        return new GetStylesRequest();
    }
}
