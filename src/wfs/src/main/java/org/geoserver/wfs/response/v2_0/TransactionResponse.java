/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response.v2_0;

import java.io.IOException;
import java.io.OutputStream;
import net.opengis.wfs20.TransactionResponseType;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.wfs.v2_0.WFS;
import org.geotools.xsd.Encoder;

public class TransactionResponse extends WFSResponse {

    public TransactionResponse(GeoServer gs) {
        super(gs, TransactionResponseType.class);
    }

    @Override
    protected void encode(Encoder encoder, Object value, OutputStream output, Operation op)
            throws IOException, ServiceException {
        encoder.encode(value, WFS.TransactionResponse, output);
    }
}
