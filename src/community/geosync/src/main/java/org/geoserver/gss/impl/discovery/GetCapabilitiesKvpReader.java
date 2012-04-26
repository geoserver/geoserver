/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.impl.discovery;

import java.util.Map;

import org.geoserver.gss.service.GetCapabilities;
import org.geoserver.ows.KvpRequestReader;

/**
 * Reads in a GetCapabilities KVP request and turns it into an appropriate internal
 * CapabilitiesRequest object.
 * 
 * @author Gabriel Roldan
 */
public class GetCapabilitiesKvpReader extends KvpRequestReader {

    public GetCapabilitiesKvpReader() {
        super(GetCapabilities.class);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public GetCapabilities read(Object req, Map kvp, Map rawKvp) throws Exception {
        GetCapabilities request = (GetCapabilities) super.read(req, kvp, rawKvp);
        request.setRawKvp(rawKvp);

        return request;
    }
}
