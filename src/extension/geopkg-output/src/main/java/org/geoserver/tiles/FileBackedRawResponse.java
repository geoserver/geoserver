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
import org.springframework.util.Assert;

public class FileBackedRawResponse extends AbstractMapResponse {
    public FileBackedRawResponse() {
        super(FileBackedRawMap.class, (Set<String>) null);
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        Assert.isInstanceOf(FileBackedRawMap.class, value);
        FileBackedRawMap map = (FileBackedRawMap) value;
        try {
            map.writeTo(output);
            output.flush();
        } finally {
            map.dispose();
        }
    }
}
