/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.map.AbstractMapResponse;

/** Writes a MapML object onto an output stream */
public class MapMLMapResponse extends AbstractMapResponse {
    private final MapMLEncoder encoder;

    /**
     * Injects the encoder to use
     *
     * @param encoder MapML encoder
     */
    public MapMLMapResponse(MapMLEncoder encoder) {
        super(MapMLMap.class, (Set<String>) null);
        this.encoder = encoder;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        MapMLMap mapmlMap = (MapMLMap) value;
        try {
            Mapml mapml = mapmlMap.getMapml();
            encoder.encode(mapml, output);
        } finally {
            mapmlMap.dispose();
        }
    }
}
