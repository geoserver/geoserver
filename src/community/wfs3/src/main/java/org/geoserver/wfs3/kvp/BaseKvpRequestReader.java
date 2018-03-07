/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.kvp;

import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.ows.Request;
import org.springframework.http.HttpHeaders;

import java.util.Map;

/**
 * Support class for common WFS3 KVP parsing needs
 */
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
        Request dispatcherRequest = Dispatcher.REQUEST.get();
        if (kvp.containsKey("f")) {
            Object format = kvp.get("f");
            setFormat(kvp, rawKvp, format);
        } else if (request != null) {
            // ignoring for the moment, until the HTML output formats are ready, otherwise
            // it won't show up in the browser
//            String header = dispatcherRequest.getHttpRequest().getHeader(HttpHeaders.ACCEPT);
//            if (header != null) {
//                String[] formats = header.split("\\s*,\\s*");
//                // TODO: check supported formats and pick the first that's actually supported
//                String format = formats[0];
//                setFormat(kvp, rawKvp, format);
//            }
        }
        
        return super.read(request, kvp, rawKvp);                            
    }

    public void setFormat(Map kvp, Map rawKvp, Object format) {
        kvp.put("format", format);
        rawKvp.put("format", format);
    }
}
