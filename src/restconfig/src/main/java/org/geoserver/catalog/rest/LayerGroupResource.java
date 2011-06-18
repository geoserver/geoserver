/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class LayerGroupResource extends AbstractCatalogResource {

    public LayerGroupResource(Context context, Request request,
            Response response, Catalog catalog) {
        super(context, request, response, LayerGroupInfo.class, catalog);
    }

    @Override
    protected Object handleObjectGet() throws Exception {
        String lg = getAttribute( "layergroup" );
        
        LOGGER.fine( "GET layer group " + lg );
        return catalog.getLayerGroupByName( lg ); 
    }

    @Override
    public boolean allowPost() {
        return getAttribute( "layergroup") == null;
    }
    
    @Override
    protected String handleObjectPost(Object object) throws Exception {
        LayerGroupInfo lg = (LayerGroupInfo) object;
        LOGGER.info( "POST layer group " + lg.getName() );
        
        if ( lg.getLayers().isEmpty() ) {
            throw new RestletException( "layer group must not be empty", Status.CLIENT_ERROR_BAD_REQUEST );
        }
       
        if ( lg.getBounds() == null ) {
            LOGGER.fine( "Auto calculating layer group bounds");
            new CatalogBuilder( catalog ).calculateLayerGroupBounds(lg);
        }
        
        catalog.add( lg );
        return lg.getName();
    }

    @Override
    public boolean allowPut() {
        return getAttribute( "layergroup") != null;
    }
    
    @Override
    protected void handleObjectPut(Object object) throws Exception {
        String layergroup = getAttribute("layergroup");
        LOGGER.info( "PUT layer group " + layergroup );
        
        LayerGroupInfo lg = (LayerGroupInfo) object;
        LayerGroupInfo original = catalog.getLayerGroupByName( layergroup );
       
        //ensure not a name change
        if ( lg.getName() != null && !lg.getName().equals( original.getName() ) ) {
            throw new RestletException( "Can't change name of a layer group", Status.CLIENT_ERROR_FORBIDDEN );
        }
        
        new CatalogBuilder( catalog ).updateLayerGroup( original, lg );
        catalog.save( original );
    }

    @Override
    public boolean allowDelete() {
        return getAttribute( "layergroup" ) != null;
    }
    
    @Override
    protected void handleObjectDelete() throws Exception {
        String layergroup = getAttribute( "layergroup" );
        LOGGER.info( "DELETE layer group " + layergroup );
        
        LayerGroupInfo lg = catalog.getLayerGroupByName( layergroup );
        catalog.remove( lg );
    }
    
    @Override
    protected void configurePersister(XStreamPersister persister, DataFormat format) {
        persister.setCallback( new XStreamPersister.Callback() {
           @Override
           protected void postEncodeReference(Object obj, String ref,
                HierarchicalStreamWriter writer, MarshallingContext context) {
            
               if ( obj instanceof StyleInfo ) {
                   encodeLink("/styles/" + encode(ref), writer);
               }
               if ( obj instanceof LayerInfo ) {
                   encodeLink("/layers/" + encode(ref), writer);
               }
           } 
        });
    }
}
