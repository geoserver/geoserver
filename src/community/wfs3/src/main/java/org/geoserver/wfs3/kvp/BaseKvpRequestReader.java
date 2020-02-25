/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.kvp;

import java.util.Map;
import org.geoserver.ows.KvpRequestReader;

/** Support class for common WFS3 KVP parsing needs */
public abstract class BaseKvpRequestReader extends KvpRequestReader {

    /**
     * Creats the new kvp request reader.
     *
     * @param requestBean The type of the request read, not <code>null</code>
     */
    public BaseKvpRequestReader(Class requestBean) {
        super(requestBean);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        if (kvp.containsKey("outputFormat")) {
            Object format = kvp.get("outputFormat");
            setOutputFormat(kvp, rawKvp, format);
        } else if (kvp.containsKey("f")) {
            Object format = kvp.get("f");
            setOutputFormat(kvp, rawKvp, format);
        }
        return super.read(request, kvp, rawKvp);
    }

    public void setOutputFormat(Map kvp, Map rawKvp, Object format) {
        kvp.put("outputFormat", format);
        rawKvp.put("outputFormat", format);
    }
}
