/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.List;
import java.util.Map;

import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
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

public class CoverageStoreResource extends AbstractCatalogResource {

    public CoverageStoreResource(Context context, Request request,
            Response response, Catalog catalog) {
        super(context, request, response, CoverageStoreInfo.class, catalog);
    }
    
    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new CoverageStoreHTMLFormat( request, response, this, catalog ); 
    }

    @Override
    protected Object handleObjectGet() throws Exception {
        String ws = getAttribute( "workspace" );
        String cs = getAttribute( "coveragestore" );
        
        LOGGER.fine( "GET coverage store " + ws + "," + cs );
        
        if ( cs == null ) {
            return catalog.getCoverageStoresByWorkspace( ws );
        }
        
        return catalog.getCoverageStoreByName( ws, cs );
    }

    @Override
    public boolean allowPost() {
        return getAttribute("coveragestore") == null;
    }
    
    @Override
    protected String handleObjectPost(Object object) throws Exception {
        CoverageStoreInfo coverageStore = (CoverageStoreInfo) object;
        catalog.add( coverageStore );
        
        LOGGER.info( "POST coverage store " + coverageStore.getName() );
        return coverageStore.getName();
    }

    @Override
    public boolean allowPut() {
        return getAttribute( "coveragestore" ) != null;
    }
    
    @Override
    protected void handleObjectPut(Object object) throws Exception {
        String workspace = getAttribute("workspace");
        String coveragestore = getAttribute("coveragestore");
        
        CoverageStoreInfo cs = (CoverageStoreInfo) object;
        CoverageStoreInfo original = catalog.getCoverageStoreByName(workspace, coveragestore);
        new CatalogBuilder( catalog ).updateCoverageStore( original, cs );
        
        catalog.save( original );
        clear(original);
        
        LOGGER.info( "PUT coverage store " + workspace + "," + coveragestore );
    }
    
    @Override
    public boolean allowDelete() {
        return getAttribute( "coveragestore" ) != null;
    }
    
    @Override
    protected void handleObjectDelete() throws Exception {
        String workspace = getAttribute("workspace");
        String coveragestore = getAttribute("coveragestore");
        boolean recurse = getQueryStringValue("recurse", Boolean.class, false);
        
        CoverageStoreInfo cs = catalog.getCoverageStoreByName(workspace, coveragestore);
        if (!recurse) {
            if ( !catalog.getCoveragesByCoverageStore(cs).isEmpty() ) {
                throw new RestletException( "coveragestore not empty", Status.CLIENT_ERROR_UNAUTHORIZED);
            }
            catalog.remove( cs );
        }
        else {
            new CascadeDeleteVisitor(catalog).visit(cs);
        }
        clear(cs);
        
        LOGGER.info( "DELETE coverage store " + workspace + "," + coveragestore );
    }

    @Override
    protected void configurePersister(XStreamPersister persister, final DataFormat format ) {
        persister.setCallback( 
            new XStreamPersister.Callback() {
                @Override
                protected void postEncodeCoverageStore(CoverageStoreInfo cs,
                        HierarchicalStreamWriter writer,
                        MarshallingContext context) {
                    //add a link to the coverages
                    writer.startNode( "coverages");
                    encodeCollectionLink("coverages", writer, format);
                    writer.endNode();
                }
                
                @Override
                protected void postEncodeReference(Object obj, String ref,
                        HierarchicalStreamWriter writer, MarshallingContext context) {
                    if ( obj instanceof WorkspaceInfo ) {
                        encodeLink( "/workspaces/" + encode(ref), writer, format );
                    }
                }
            }
        );
    }
    
    void clear(CoverageStoreInfo info) {
        catalog.getResourcePool().clear(info);
    }
    
    static class CoverageStoreHTMLFormat extends CatalogFreemarkerHTMLFormat {
        Catalog catalog;
        
        public CoverageStoreHTMLFormat(Request request,
                Response response, Resource resource, Catalog catalog) {
            super(CoverageStoreInfo.class, request, response, resource);
            this.catalog = catalog;
        }

        @Override
        protected Configuration createConfiguration(Object data, Class clazz) {
            Configuration cfg = 
                super.createConfiguration(data, clazz);
            cfg.setObjectWrapper(new ObjectToMapWrapper<CoverageStoreInfo>(CoverageStoreInfo.class) {
                @Override
                protected void wrapInternal(Map properties, SimpleHash model, CoverageStoreInfo object) {
                    List<CoverageInfo> coverages = catalog.getCoveragesByCoverageStore(object);
                    
                    properties.put( "coverages", new CollectionModel( coverages, new ObjectToMapWrapper(CoverageInfo.class) ) );
                }
            });
            
            return cfg;
        }
    };

}
