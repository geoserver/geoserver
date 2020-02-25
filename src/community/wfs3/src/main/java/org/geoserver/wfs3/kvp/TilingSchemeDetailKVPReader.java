/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.kvp;

import java.util.Map;
import org.geoserver.wfs3.TilingSchemeDescriptionRequest;
import org.geowebcache.grid.GridSet;

public class TilingSchemeDetailKVPReader extends BaseKvpRequestReader {

    public TilingSchemeDetailKVPReader() {
        super(TilingSchemeDescriptionRequest.class);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        TilingSchemeDescriptionRequest req =
                (TilingSchemeDescriptionRequest) super.read(request, kvp, rawKvp);
        if (kvp.containsKey("tilingScheme")) {
            req.setGridSet((GridSet) kvp.get("tilingScheme"));
        }
        return req;
    }
}
