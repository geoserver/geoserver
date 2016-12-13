/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.List;
import java.util.Map;

import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import freemarker.ext.beans.CollectionModel;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;

public class WMSStoreResource extends AbstractCatalogResource {

    public WMSStoreResource(Context context, Request request, Response response, Catalog catalog) {
        super(context, request, response, WMSStoreInfo.class, catalog);
    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new WMSStoreHTMLFormat( request, response, this, catalog );
    }
    
    @Override
    protected Object handleObjectGet() {
        String ws = getAttribute( "workspace" );
        String wms = getAttribute( "wmsstore" );
        
        WorkspaceInfo wsInfo = catalog.getWorkspaceByName(ws);
        
        LOGGER.fine( "GET wms store " + ws + "," + wms );
        return catalog.getStoreByName( wsInfo, wms, WMSStoreInfo.class );
    }

    @Override
    public boolean allowPost() {
        return getAttribute("wmsstore") == null;
    }
    
    @Override
    protected String handleObjectPost(Object object) throws Exception {
        String workspace = getAttribute( "workspace" );

        WMSStoreInfo wms = (WMSStoreInfo) object;
        if ( wms.getWorkspace() != null ) {
             //ensure the specifried workspace matches the one dictated by the uri
             WorkspaceInfo ws = (WorkspaceInfo) wms.getWorkspace();
             if ( !workspace.equals( ws.getName() ) ) {
                 throw new RestletException( "Expected workspace " + workspace + 
                     " but client specified " + ws.getName(), Status.CLIENT_ERROR_FORBIDDEN );
             }
        } else {
             wms.setWorkspace( catalog.getWorkspaceByName( workspace ) );
        } 
        wms.setEnabled(true);
        
        catalog.validate(wms, false).throwIfInvalid();
        catalog.add( wms );
        
        LOGGER.info( "POST WSM store " + wms.getName() );
        return wms.getName();
    }

    @Override
    public boolean allowPut() {
        return getAttribute( "wmsstore" ) != null;
    }
    
    @Override
    protected void handleObjectPut(Object object) throws Exception {
        String workspace = getAttribute("workspace");
        String wmsstore = getAttribute("wmsstore");
        
        WMSStoreInfo wms = (WMSStoreInfo) object;
        WMSStoreInfo original = catalog.getStoreByName(workspace, wmsstore, WMSStoreInfo.class);
        
        //ensure this is not a name or workspace change
        if ( wms.getName() != null && !wms.getName().equals( original.getName() ) ) {
            throw new RestletException( "Can't change name of data store.", Status.CLIENT_ERROR_FORBIDDEN );
        }
        if ( wms.getWorkspace() != null && !wms.getWorkspace().equals( original.getWorkspace() ) ) {
            throw new RestletException( "Can't change workspace of data store.", Status.CLIENT_ERROR_FORBIDDEN );
        }
        
        new CatalogBuilder( catalog ).updateWMSStore( original, wms );
        
        catalog.validate(original, false).throwIfInvalid();
        catalog.save( original );
        
        LOGGER.info( "PUT wms store " + workspace + "," + wmsstore );
    }
    
    @Override
    public boolean allowDelete() {
        return getAttribute( "wmsstore" ) != null;
    }
    
    @Override
    protected void handleObjectDelete() throws Exception {
        String workspace = getAttribute("workspace");
        String wmsstore = getAttribute("wmsstore");
        boolean recurse = getQueryStringValue("recurse", Boolean.class, false);
        
        WMSStoreInfo wms = catalog.getStoreByName(workspace, wmsstore, WMSStoreInfo.class);
        if (!recurse) {
            if ( !catalog.getResourcesByStore(wms, WMSLayerInfo.class).isEmpty() ) {
                throw new RestletException( "store not empty", Status.CLIENT_ERROR_FORBIDDEN);
            }
            catalog.remove( wms );
        }
        else {
            new CascadeDeleteVisitor(catalog).visit(wms);
        }
        
         
        LOGGER.info( "DELETE wms store " + workspace + "," + wmsstore );
    }
    
    @Override
    protected void configurePersister(XStreamPersister persister, DataFormat format) {
        persister.setCallback( 
            new XStreamPersister.Callback() {
                @Override
                protected Class<WMSStoreInfo> getObjectClass() {
                    return WMSStoreInfo.class;
                }
                @Override
                protected CatalogInfo getCatalogObject() {
                    String workspace = getAttribute("workspace");
                    String wmsstore = getAttribute("wmsstore");
                    
                    if (workspace == null || wmsstore == null) {
                        return null;
                    }
                    return catalog.getStoreByName(workspace, wmsstore, WMSStoreInfo.class);
                }
                @Override
                protected void postEncodeWMSStore(WMSStoreInfo ds,
                        HierarchicalStreamWriter writer,
                        MarshallingContext context) {
                    //add a link to the wms layers
                    writer.startNode( "wmsLayers");
                    encodeCollectionLink("wmslayers", writer);
                    writer.endNode();
                }
                @Override
                protected void postEncodeReference(Object obj, String ref, String prefix,
                        HierarchicalStreamWriter writer, MarshallingContext context) {
                    if ( obj instanceof WorkspaceInfo ) {
                        encodeLink("/workspaces/" + ref, writer );
                    }
                }
            }
        );
    }
    
    static class WMSStoreHTMLFormat extends CatalogFreemarkerHTMLFormat {
        Catalog catalog;
        
        public WMSStoreHTMLFormat(Request request,
                Response response, Resource resource, Catalog catalog) {
            super(WMSStoreInfo.class, request, response, resource);
            this.catalog = catalog;
        }

        @Override
        protected Configuration createConfiguration(Object data, Class clazz) {
            Configuration cfg = 
                super.createConfiguration(data, clazz);
            cfg.setObjectWrapper(new ObjectToMapWrapper<WMSStoreInfo>(WMSStoreInfo.class) {
                @Override
                protected void wrapInternal(Map properties, SimpleHash model, WMSStoreInfo object) {
                    List<WMSLayerInfo> wmsLayers = catalog.getResourcesByStore(object, WMSLayerInfo.class);
                    
                    properties.put( "wmsLayers", new CollectionModel( wmsLayers, new ObjectToMapWrapper(WMSLayerInfo.class) ) );
                }
            });
            
            return cfg;
        }
    }


}
