/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.response;

import java.io.IOException;
import java.io.OutputStream;

import org.geoserver.gss.PostDiffResponseType;
import org.geoserver.gss.xml.GSS;
import org.geoserver.gss.xml.GSSConfiguration;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.xml.Encoder;

/**
 * Encodes a {@link PostDiffResponseType} into an XML response
 * 
 * @author aaime
 * 
 */
public class PostDiffResponseOutputFormat extends Response {

    GSSConfiguration configuration;

    GSS gss;

    public PostDiffResponseOutputFormat(GSSConfiguration configuration, GSS gss) {
        super(PostDiffResponseType.class);
        this.configuration = configuration;
        this.gss = gss;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "text/xml";
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException,
            ServiceException {
        PostDiffResponseType response = (PostDiffResponseType) value;
        Encoder encoder = new Encoder(configuration, gss.getSchema());
        encoder.setIndenting(true);

        encoder.encode(response, GSS.PostDiffResponse, output);
    }

}
