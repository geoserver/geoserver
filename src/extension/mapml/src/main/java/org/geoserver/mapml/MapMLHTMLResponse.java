/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.map.AbstractMapResponse;

/** A Response containing a MapML HTML document */
public class MapMLHTMLResponse extends AbstractMapResponse {
    /** Registers with the parent class the type of object that will be written to the output stream */
    public MapMLHTMLResponse() {
        super(MapMLHTMLMap.class, (Set<String>) null);
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException, ServiceException {
        MapMLHTMLMap mapmlHTMLMap = (MapMLHTMLMap) value;
        try {
            String mapmlHTML = mapmlHTMLMap.getMapmlHTML();
            output.write(mapmlHTML.getBytes());
        } finally {
            mapmlHTMLMap.dispose();
        }
    }
}
