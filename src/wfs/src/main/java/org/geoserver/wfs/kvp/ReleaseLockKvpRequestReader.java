/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import java.util.Map;
import org.geoserver.ows.KvpRequestReader;

public class ReleaseLockKvpRequestReader extends KvpRequestReader {
    public ReleaseLockKvpRequestReader() {
        super(String.class);
    }

    public Object createRequest() throws Exception {
        return "";
    }

    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        return kvp.get("lockId");
    }
}
