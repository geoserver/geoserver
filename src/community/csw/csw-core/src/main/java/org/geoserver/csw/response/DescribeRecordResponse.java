/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.response;

import java.io.IOException;
import java.io.OutputStream;

import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.opengis.feature.type.FeatureType;

/**
 * Encodes the DescribeRecord response
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class DescribeRecordResponse extends Response {

    public DescribeRecordResponse() {
        super(FeatureType[].class, "application/xml");
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "application/xml";
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException,
            ServiceException {
        // TODO Auto-generated method stub

    }

}
