/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.MapJSONFormat;
import org.geoserver.rest.format.MapXMLFormat;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * Base class for resources which transform an underlying target object into a java map.
 *
 * @author David Winslow <dwinslow@openplans.org>
 */
public abstract class MapResource extends AbstractResource {
    /**
     * logger
     */
    protected static Logger LOG = org.geotools.util.logging.Logging.getLogger("org.geoserver.rest");
    
    public MapResource(Context context, Request request, Response response) {
        super(context, request, response);
    }
    
    public MapResource() {
    }

    /**
     * Creates the list of formats for serialization and de-serialization.
     * <p>
     * This implementation adds support for XML and JSON via the {@link MapXMLFormat} and 
     * {@link MapJSONFormat} classes respectively. Subclasses should override/extend as needed.
     * </p>
     */
    @Override
    protected List<DataFormat> createSupportedFormats(Request request,
            Response response) {
        ArrayList formats = new ArrayList();
        formats.add( new MapXMLFormat() );
        formats.add( new MapJSONFormat() );
        return formats;
    }
    
    /**
     * Handles a request using the GET method.
     * <p>
     * This method operates by obtaining the map representation of the target object via {@link #getMap()}
     * and then serializing the map in the request format.
     * </p>
     */
    public void handleGet() {
        Map map;
        try {
            map = getMap();
        } 
        catch (Exception e) {
            throw new RestletException( "", Status.SERVER_ERROR_INTERNAL, e );
        }
        DataFormat format = getFormatGet();
        getResponse().setEntity(format.toRepresentation(map));
    }

    /**
     * Returns the map representation of the underlying target object in a GET request.
     * <p>
     * This method is called by {@link #handleGet()}. 
     * </p>
     * 
     * @return A map representing the properties of the underlying target object.
     */
    public abstract Map getMap() throws Exception;

    /**
     * Handles a request using the POST method.
     * <p>
     * This method operates by de-serializing the map representation of the target object and 
     * then calling {@link #postMap(Map)}. 
     * </p>
     */
    @Override
    public void handlePost() {
        DataFormat format = getFormatPostOrPut();
        Map map = (Map) format.toObject(getRequest().getEntity());
        try {
            postMap(map);
        } 
        catch (Exception e) {
            throw new RestletException( "", Status.SERVER_ERROR_INTERNAL, e );
        }
    }
    
    /**
     * Handles the map result of a POST request.
     * <p>
     * If subclasses choose to support the POST method they must override this method as well as
     * {@link #allowPost()}.
     * </p>
     * @param map The de-serialized map representation of the content being POST'd.
     */
    protected void postMap(Map map) throws Exception {
    }
    
    /**
     * Handles a request using the PUT method.
     * <p>
     * This method operates by de-serializing the map representation of the target object and 
     * then calling {@link #putMap(Map)}. 
     * </p>
     */
    @Override
    public void handlePut() {
        DataFormat format = getFormatPostOrPut();
        Map map = (Map) format.toObject(getRequest().getEntity());
        try {
            putMap(map);
        } 
        catch (Exception e) {
            throw new RestletException( "", Status.SERVER_ERROR_INTERNAL, e );
        }
    }

    /**
     * Handles the map result of a PUT request.
     * <p>
     * If subclasses choose to support the PUT method they must override this method as well as
     * {@link #allowPut()}.
     * </p>
     * @param map The de-serialized map representation of the content being PUT'd..
     */
    protected void putMap(Map map) throws Exception {
    }
}
