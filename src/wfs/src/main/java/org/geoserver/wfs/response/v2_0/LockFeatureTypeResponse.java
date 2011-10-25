/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response.v2_0;

import java.io.IOException;
import java.io.OutputStream;

import net.opengis.wfs20.LockFeatureResponseType;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.wfs.v2_0.WFS;
import org.geotools.xml.Encoder;

public class LockFeatureTypeResponse extends WFSResponse {
    
    public LockFeatureTypeResponse(GeoServer gs) {
        super(gs, LockFeatureResponseType.class);
    }

    @Override
    protected void encode(Encoder encoder, Object value, OutputStream output, Operation op) 
        throws IOException, ServiceException {
        encoder.encode(value, WFS.LockFeatureResponse, output);
    }
}
