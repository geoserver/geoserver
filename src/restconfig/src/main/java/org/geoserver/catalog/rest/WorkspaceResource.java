/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
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
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.PageInfo;
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

public class WorkspaceResource extends AbstractCatalogResource {

    public WorkspaceResource(Context context, Request request, Response response, Catalog catalog) {
        super(context, request, response, WorkspaceInfo.class, catalog);
    }
    
    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new WorkspaceHTMLFormat(request,response,this,catalog);
    }
    
    @Override
    protected void configurePersister(XStreamPersister persister, DataFormat format) {
        persister.setCallback( new XStreamPersister.Callback() {
            @Override
            protected void postEncodeWorkspace(WorkspaceInfo ws,
                    HierarchicalStreamWriter writer, MarshallingContext context) {
                //add a link to the stores
                writer.startNode( "dataStores");
                encodeCollectionLink("datastores", writer);
                writer.endNode();
                
                writer.startNode( "coverageStores");
                encodeCollectionLink("coveragestores", writer);
                writer.endNode();
                
                writer.startNode( "wmsStores");
                encodeCollectionLink("wmsstores", writer);
                writer.endNode();
            }
        });
    }
    
    @Override
    public boolean allowGet() {
        return getAttribute( "workspace" ) != null;
    }
    
    @Override
    protected Object handleObjectGet() {
        String ws = getAttribute( "workspace" );
       
        LOGGER.fine( "GET workspace " + ws);
        
       return catalog.getWorkspaceByName( ws );
    }
    
    @Override
    public boolean allowPost() {
        return getAttribute( "workspace") == null;
    }
    
    @Override
    protected String handleObjectPost(Object object) throws Exception {
        WorkspaceInfo workspace = (WorkspaceInfo) object;
        catalog.add( workspace );
        
        //create a namespace corresponding to the workspace if one does not 
        // already exist
        NamespaceInfo namespace = catalog.getNamespaceByPrefix( workspace.getName() );
        if ( namespace == null ) {
            LOGGER.fine( "Automatically creating namespace for workspace " + workspace.getName() );

            namespace = catalog.getFactory().createNamespace();
            namespace.setPrefix( workspace.getName() );
            namespace.setURI( "http://" + workspace.getName() );
            catalog.add( namespace );
        }
        
        LOGGER.info( "POST workspace " + workspace.getName() );
        return workspace.getName();
    }
    
    @Override
    public boolean allowPut() {
        return getAttribute( "workspace") != null;
    }
    
    @Override
    protected void handleObjectPut(Object object) throws Exception {
        WorkspaceInfo workspace = (WorkspaceInfo) object;
        String ws = getAttribute("workspace");
        
        if ( "default".equals( ws ) ) {
            catalog.setDefaultWorkspace( workspace );
        } else {
            WorkspaceInfo original = catalog.getWorkspaceByName( ws );
            
            //ensure this is not a name change
            if ( workspace.getName() != null && !workspace.getName().equals( original.getName() ) ) {
                throw new RestletException( "Can't change the name of a workspace.", Status.CLIENT_ERROR_FORBIDDEN );
            }
            
            new CatalogBuilder(catalog).updateWorkspace( original, workspace );
            catalog.save( original );
        }
        
        LOGGER.info( "PUT workspace " + ws );
    }
    
    @Override
    public boolean allowDelete() {
        return getAttribute( "workspace" ) != null && !"default".equals( getAttribute("workspace"));
    }
    
    @Override
    protected void handleObjectDelete() throws Exception {
        String workspace = getAttribute("workspace");
        boolean recurse = getQueryStringValue("recurse", Boolean.class, false);
        
        WorkspaceInfo ws = catalog.getWorkspaceByName( workspace );
        
        if (!recurse) {
            if ( !catalog.getStoresByWorkspace(ws, StoreInfo.class).isEmpty() ) {
                throw new RestletException( "Workspace not empty", Status.CLIENT_ERROR_FORBIDDEN );
            }
            
            //check for "linked" workspace
            NamespaceInfo ns = catalog.getNamespaceByPrefix( ws.getName() );
            if ( ns != null ) {
                if ( !catalog.getFeatureTypesByNamespace( ns ).isEmpty() ) {
                    throw new RestletException( "Namespace for workspace not empty.", Status.CLIENT_ERROR_FORBIDDEN );
                }
                catalog.remove( ns );
            }
            
            catalog.remove( ws );
        }
        else {
            //recursive delete
            new CascadeDeleteVisitor(catalog).visit(ws);
        }
        
        LOGGER.info( "DELETE workspace " + ws );
    }
    
    static class WorkspaceHTMLFormat extends CatalogFreemarkerHTMLFormat {
    
        Catalog catalog;
        
        public WorkspaceHTMLFormat(Request request, Response response, Resource resource, Catalog catalog ) {
            super(WorkspaceInfo.class, request, response, resource);
            this.catalog = catalog;
        }
        
        @Override
        protected Configuration createConfiguration(Object data, Class clazz) {
            Configuration cfg = 
                super.createConfiguration(data, clazz);
            cfg.setObjectWrapper(new ObjectToMapWrapper<WorkspaceInfo>(WorkspaceInfo.class) {
                @Override
                protected void wrapInternal(Map properties, SimpleHash model, WorkspaceInfo object) {
                    List<DataStoreInfo> dataStores = catalog.getStoresByWorkspace(object, DataStoreInfo.class);
                    properties.put( "dataStores", new CollectionModel( dataStores, new ObjectToMapWrapper(DataStoreInfo.class) ) );
                    
                    List<CoverageStoreInfo> coverageStores = catalog.getStoresByWorkspace(object, CoverageStoreInfo.class);
                    properties.put( "coverageStores", new CollectionModel( coverageStores, new ObjectToMapWrapper(CoverageStoreInfo.class) ) );
                    
                    List<WMSStoreInfo> wmsStores = catalog.getStoresByWorkspace(object, WMSStoreInfo.class);
                    properties.put( "wmsStores", new CollectionModel( wmsStores, new ObjectToMapWrapper(WMSStoreInfo.class) ) );
                    
                    properties.put( "isDefault",  object.equals( catalog.getDefaultWorkspace() ) );
                }
            });
            
            return cfg;
        }
    };
}
