/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.springframework.util.Assert;

/**
 * A {@link Response} to handle a {@link RawMap}
 *
 * @author Gabriel Roldan
 * @see RawMap
 */
public class RawMapResponse extends AbstractMapResponse {

    public RawMapResponse() {
        super(RawMap.class, (Set<String>) null);
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        Assert.isInstanceOf(RawMap.class, value);
        RawMap map = (RawMap) value;
        try {
            map.writeTo(output);
            output.flush();
        } finally {
            map.dispose();
        }
    }
}
