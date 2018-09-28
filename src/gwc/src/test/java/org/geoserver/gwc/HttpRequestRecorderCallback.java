/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;

/** Integration tests support class that keeps track of the requests send to the dispatcher. */
public final class HttpRequestRecorderCallback extends AbstractDispatcherCallback {

    static List<HttpServletRequest> requests = new ArrayList<>();

    public static void reset() {
        synchronized (requests) {
            requests.clear();
        }
    }

    public static ArrayList<HttpServletRequest> getRequests() {
        synchronized (requests) {
            return new ArrayList<>(requests);
        }
    }

    @Override
    public Request init(Request request) {
        synchronized (requests) {
            requests.add(request.getHttpRequest());
        }
        return super.init(request);
    }
}
