/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.sld;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;
import org.vfny.geoserver.Request;
import org.vfny.geoserver.util.requests.readers.KvpRequestReader;


public class PutStylesKvpReader extends KvpRequestReader {


    private WMS config;

    public PutStylesKvpReader(Map kvpPairs, WMS service) {
        super(kvpPairs, service.getServiceInfo());
        this.config = service;
    }

    public Request getRequest(HttpServletRequest httpRequest)
        throws ServiceException {
        PutStylesRequest request = new PutStylesRequest((WMS) serviceConfig);
        request.setHttpServletRequest(httpRequest);

        String version = getRequestVersion();
        request.setVersion(version);

        parseMandatoryParameters(request);
        parseOptionalParameters(request);

        return request;
    }

    public void parseMandatoryParameters(PutStylesRequest request)
        throws SldException {
        String req = getValue("REQUEST");

        if ((req != null) && !req.equals("")) {
            if (!req.equalsIgnoreCase("PutStyles")) {
                throw new SldException("Expecting 'request=PutStyles'");
            }
        }

        String mode = getValue("MODE");

        if ((mode != null) && !mode.equals("")) {
            if (mode.equalsIgnoreCase("InsertAndReplace") || mode.equalsIgnoreCase("ReplaceAll")) {
                request.setMode(mode);
            } else {
                throw new SldException("Parameter must be 'InsertAndReplace' or 'ReplaceAll'.");
            }
        }
    }

    public void parseOptionalParameters(PutStylesRequest request) {
        String sld = getValue("SLD");

        if ((sld != null) && !sld.equals("")) {
            request.setSLD(sld);
        }

        String sld_body = getValue("SLD_BODY");

        if ((sld_body != null) && !sld_body.equals("")) {
            request.setSldBody(sld_body);
        }
    }

    protected String getRequestVersion() {
        String version = getValue("VERSION");

        if (version == null) {
            version = getValue("WMTVER");
        }

        if (version == null) {
            version = config.getVersion();
        }

        return version;
    }
}
