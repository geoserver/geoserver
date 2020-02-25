/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.xml.v2_0_2;

import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.csw.CSWConfiguration;

/**
 * The GetRecords request has a "validate" mode in which we have to return an Acknowledgement with
 * the verbatim request, thus we have to store it
 */
public class CSWRecordingXmlReader extends CSWXmlReader implements DispatcherCallback {

    public static final ThreadLocal<String> RECORDED_REQUEST = new ThreadLocal<String>();

    public CSWRecordingXmlReader(
            String element,
            String version,
            CSWConfiguration configuration,
            EntityResolverProvider resolverProvider) {
        super(element, version, configuration, resolverProvider);
    }

    public Object read(Object request, Reader reader, Map kvp) throws Exception {
        String requestText = IOUtils.toString(reader);
        RECORDED_REQUEST.set(requestText);
        return super.read(requestText, new StringReader(requestText), kvp);
    }

    @Override
    public Request init(Request request) {
        return request;
    }

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        return service;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        return operation;
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        return result;
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        return response;
    }

    @Override
    public void finished(Request request) {
        RECORDED_REQUEST.remove();
    }
}
