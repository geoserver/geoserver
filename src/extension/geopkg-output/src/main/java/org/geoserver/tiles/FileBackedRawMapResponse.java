/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.tiles;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.map.AbstractMapResponse;
import org.geoserver.wms.map.RawMapResponse;
import org.springframework.util.Assert;

/**
 * MapResponse for a response backed by a file (i.e. geopkg).
 *
 * <p>*
 *
 * <p>See {@link RawMapResponse}, which is similar, but backed with a byte[]
 */
public class FileBackedRawMapResponse extends AbstractMapResponse {
    public FileBackedRawMapResponse() {
        super(FileBackedRawMap.class, (Set<String>) null);
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        // offset work to FileBackedRawMap's writeTo() method
        Assert.isInstanceOf(FileBackedRawMap.class, value);

        try (FileBackedRawMap map = (FileBackedRawMap) value) {
            map.writeTo(output);
            output.flush();
        }
    }
}
