/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import java.util.Map;

import net.opengis.wfs.GetCapabilitiesType;
import net.opengis.wfs.WfsFactory;

import org.eclipse.emf.ecore.EFactory;
import org.geoserver.wfs.request.GetCapabilitiesRequest;

public class GetCapabilitiesKvpRequestReader extends WFSKvpRequestReader {

    public GetCapabilitiesKvpRequestReader() {
        this(GetCapabilitiesType.class, WfsFactory.eINSTANCE);
    }
    
    public GetCapabilitiesKvpRequestReader(Class requestBean, EFactory factory) {
        super(requestBean, factory);
    }
    
    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        request = super.read(request, kvp, rawKvp);

        //set the version attribute on the request
        if (kvp.containsKey("version")) {
            GetCapabilitiesRequest req = GetCapabilitiesRequest.adapt(request);
            //TODO: put this check in a cite hack
            if (req.getAcceptVersions() == null || req.getAcceptVersions().isEmpty()) {
                req.setAcceptVersions((String)kvp.get("version"));    
            }
        }

        return request;
    }
}
