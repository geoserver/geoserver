/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xslt.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.StreamDataFormat;
import org.restlet.data.MediaType;
import org.restlet.data.Status;

public class XSLTDataFormat extends StreamDataFormat {

    protected XSLTDataFormat(MediaType mediaType) {
        super(mediaType);
    }

    @Override
    protected Object read(InputStream in) throws IOException {
        // store in a string so that we can perform validation
        String xslt = IOUtils.toString(in);

        try {
            Source xslSource = new StreamSource(new StringReader(xslt));
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.newTemplates(xslSource);
        } catch (Exception e) {
            throw new RestletException("Invalid XSLT : " + e.getMessage(),
                    Status.CLIENT_ERROR_BAD_REQUEST);
        }

        return xslt;
    }

    @Override
    protected void write(Object object, OutputStream out) throws IOException {
        IOUtils.copy((InputStream) object, out);
    }

}
