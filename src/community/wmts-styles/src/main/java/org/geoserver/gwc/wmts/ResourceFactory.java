/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Returns a Resource for a given request, or null if not matched */
interface ResourceFactory {

    Resource getResourceFor(HttpServletRequest request);

    interface Resource {
        void get(HttpServletResponse response) throws IOException;

        void put(HttpServletResponse response) throws IOException;

        void delete(HttpServletResponse response) throws IOException;
    }
}
