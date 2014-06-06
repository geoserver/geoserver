/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.opengeo.gsr.core.exception.ServiceError;
import org.opengeo.gsr.core.exception.ServiceException;
import org.restlet.Filter;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class JsonpFilter extends Filter {

    @Override
    protected void afterHandle(Request request, Response response) {
        String callback = request.getResourceRef().getQueryAsForm().getFirstValue("callback");
        if (callback != null) {
            StringBuilder stringBuilder = new StringBuilder(callback);
            stringBuilder.append("(");
            Representation representation = response.getEntity();
            if (representation != null) {
                try {
                    InputStream inputStream = representation.getStream();
                    if (inputStream != null) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        byte[] bytes = new byte[0x10000];
                        int length;
                        while ((length = inputStream.read(bytes)) > 0) {
                            out.write(bytes, 0, length);
                        }
                        stringBuilder.append(out.toString("UTF-8"));
                    }
                } catch (IOException e) {
                    List<String> details = new ArrayList<String>();
                    details.add(e.getMessage());
                    ServiceException serviceException = new ServiceException(new ServiceError(
                            (Status.SERVER_ERROR_INTERNAL.getCode()), "Internal Server Error",
                            details));
                    response.setEntity(serviceException);
                }
            }
            stringBuilder.append(")");
            response.setEntity(new StringRepresentation(stringBuilder.toString(),
                    MediaType.TEXT_PLAIN));
        }

    }
}
