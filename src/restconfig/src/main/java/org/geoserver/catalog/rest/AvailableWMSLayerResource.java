/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.ReflectiveXMLFormat;
import org.geotools.data.ows.Layer;
import org.geotools.data.wms.WebMapServer;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.thoughtworks.xstream.XStream;

public class AvailableWMSLayerResource extends AbstractCatalogResource {

    public AvailableWMSLayerResource(Context context, Request request, Response response, Catalog catalog) {
        super(context, request, response, FeatureTypeInfo.class, catalog);
    }

    @Override
    protected Object handleObjectGet() {
        String workspace = (String) getRequest().getAttributes().get( "workspace" );
        String wmsstore = (String) getRequest().getAttributes().get( "wmsstore" );
        
        WMSStoreInfo info = catalog.getStoreByName( workspace, wmsstore, WMSStoreInfo.class );
        if ( info == null ) {
            throw new RestletException( "No such WMS store: " + wmsstore, Status.CLIENT_ERROR_NOT_FOUND );
        }
        
        //list of available feature types
        List<String> available = new ArrayList<String>();
        try {
            WebMapServer ds = info.getWebMapServer(null);
            
            for ( Layer layer : ds.getCapabilities().getLayerList() ) {
                if(layer.getName() == null || "".equals(layer.getName())) {
                    continue;
                }
                    
                WMSLayerInfo wIinfo = catalog.getResourceByStore(info, layer.getName(), WMSLayerInfo.class);
                if (wIinfo == null ) {
                    //not in catalog, add it
                    available.add( layer.getName() );
                }
            }
        } 
        catch (IOException e) {
            throw new RestletException( "Could not load wms store: " + wmsstore, Status.SERVER_ERROR_INTERNAL, e );
        }
        
        return available;
    }
    
    
    @Override
    protected ReflectiveXMLFormat createXMLFormat(Request request, Response response) {
        return new ReflectiveXMLFormat() {
          
            @Override
            protected void write(Object data, OutputStream output)
                    throws IOException {
                XStream xstream = new SecureXStream();
                xstream.alias( "wmsLayerName", String.class);
                xstream.toXML( data, output );
            }
        };
    }
    
    @Override
    public boolean allowPost() {
        return false;
    }
    
    @Override
    protected String handleObjectPost(Object object) {
        return null;
    }

    @Override
    protected void handleObjectPut(Object object) {
        //do nothing, we do not allow post
    }
}
