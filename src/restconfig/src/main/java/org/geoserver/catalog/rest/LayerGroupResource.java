/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DataStoreInfo;
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
        String ws = getAttribute("workspace");
        String lg = getAttribute( "layergroup" );
        
        LOGGER.fine( "GET layer group " + lg );
        return ws == null ? catalog.getLayerGroupByName( lg ) : catalog.getLayerGroupByName(ws,lg); 
    }

    @Override
    public boolean allowPost() {
        if (getAttribute("workspace") == null && !isAuthenticatedAsAdmin()) {
            return false;
        }
        return getAttribute( "layergroup") == null;
    }
    
    @Override
    protected String handleObjectPost(Object object) throws Exception {
        String ws = getAttribute("workspace");

        LayerGroupInfo lg = (LayerGroupInfo) object;
        LOGGER.info( "POST layer group " + lg.getName() + ws != null ? " to workspace " + ws : "");
        
        if ( lg.getLayers().isEmpty() ) {
            throw new RestletException( "layer group must not be empty", Status.CLIENT_ERROR_BAD_REQUEST );
        }
       
        if ( lg.getBounds() == null ) {
            LOGGER.fine( "Auto calculating layer group bounds");
            new CatalogBuilder( catalog ).calculateLayerGroupBounds(lg);
        }

        if (ws != null) {
            lg.setWorkspace(catalog.getWorkspaceByName(ws));
        }

        if (lg.getMode() == null) {
            LOGGER.fine("Setting layer group mode SINGLE");
            lg.setMode(LayerGroupInfo.Mode.SINGLE);
        }
        
        catalog.add( lg );
        return lg.getName();
    }

    @Override
    public boolean allowPut() {
        //global layer groups can only be edited by full admin
        if (getAttribute("workspace") == null && !isAuthenticatedAsAdmin()) {
            return false;
        }
        return getAttribute( "layergroup") != null;
    }
    
    @Override
    protected void handleObjectPut(Object object) throws Exception {
        String workspace = getAttribute("workspace");
        String layergroup = getAttribute("layergroup");

        LOGGER.info( "PUT layer group " + layergroup 
                + workspace == null ? ", workspace " + workspace : "");
        
        LayerGroupInfo lg = (LayerGroupInfo) object;
        LayerGroupInfo original = catalog.getLayerGroupByName(workspace, layergroup );
       
        //ensure not a name change
        if ( lg.getName() != null && !lg.getName().equals( original.getName() ) ) {
            throw new RestletException( "Can't change name of a layer group", Status.CLIENT_ERROR_FORBIDDEN );
        }

        //ensure not a workspace change
        if (lg.getWorkspace() != null) {
            if (!lg.getWorkspace().equals(original.getWorkspace())) {
                throw new RestletException( "Can't change the workspace of a layer group, instead " +
                    "DELETE from existing workspace and POST to new workspace", Status.CLIENT_ERROR_FORBIDDEN );
            }
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
        String workspace = getAttribute("workspace");
        String layergroup = getAttribute( "layergroup" );
        LOGGER.info( "DELETE layer group " + layergroup );
        
        LayerGroupInfo lg = workspace == null ? catalog.getLayerGroupByName( layergroup ) : 
            catalog.getLayerGroupByName(workspace, layergroup);
                
        catalog.remove( lg );
    }
    
    @Override
    protected void configurePersister(XStreamPersister persister, DataFormat format) {
        persister.setCallback( new XStreamPersister.Callback() {
            @Override
            protected Class<LayerGroupInfo> getObjectClass() {
                return LayerGroupInfo.class;
            }
            @Override
            protected CatalogInfo getCatalogObject() {
                String workspace = getAttribute("workspace");
                String layergroup = getAttribute("layergroup");
                
                if (layergroup == null) {
                    return null;
                }
                return catalog.getLayerGroupByName(workspace, layergroup );
            }
           @Override
           protected void postEncodeReference(Object obj, String ref, String prefix,
                HierarchicalStreamWriter writer, MarshallingContext context) {
            
               if ( obj instanceof StyleInfo ) {
                   StringBuffer link = new StringBuffer();
                   if (prefix != null) {
                       link.append("/workspaces/").append(encode(prefix));
                   }
                   link.append("/styles/").append(encode(ref));
                   encodeLink(link.toString(), writer);
               }
               if ( obj instanceof LayerInfo ) {
                   encodeLink("/layers/" + encode(ref), writer);
               }
           } 
        });
    }
}
