/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geotools.data.DataAccessFactory;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;
import org.vfny.geoserver.util.DataStoreUtils;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import freemarker.ext.beans.CollectionModel;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;

public class DataStoreResource extends AbstractCatalogResource {

    public DataStoreResource(Context context, Request request, Response response, Catalog catalog) {
        super(context, request, response, DataStoreInfo.class, catalog);
    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new DataStoreHTMLFormat( request, response, this, catalog );
    }
    
    @Override
    protected Object handleObjectGet() {
        String ws = getAttribute( "workspace" );
        String ds = getAttribute( "datastore" );
        
        WorkspaceInfo wsInfo = catalog.getWorkspaceByName(ws);
        
        LOGGER.fine( "GET data store " + ws + "," + ds );
        return catalog.getDataStoreByName( wsInfo, ds );
    }

    @Override
    public boolean allowPost() {
        return getAttribute("datastore") == null;
    }
    
    @Override
    protected String handleObjectPost(Object object) throws Exception {
        String workspace = getAttribute( "workspace" );

        DataStoreInfo ds = (DataStoreInfo) object;
        if ( ds.getWorkspace() != null ) {
             //ensure the specifried workspace matches the one dictated by the uri
             WorkspaceInfo ws = (WorkspaceInfo) ds.getWorkspace();
             if ( !workspace.equals( ws.getName() ) ) {
                 throw new RestletException( "Expected workspace " + workspace + 
                     " but client specified " + ws.getName(), Status.CLIENT_ERROR_FORBIDDEN );
             }
        }
        else {
             ds.setWorkspace( catalog.getWorkspaceByName( workspace ) );
        } 
        ds.setEnabled(true);
        
        //if no namespace parameter set, set it
        //TODO: we should really move this sort of thing to be something central
        if (!ds.getConnectionParameters().containsKey("namespace")) {
            WorkspaceInfo ws = ds.getWorkspace();
            NamespaceInfo ns = catalog.getNamespaceByPrefix(ws.getName());
            if (ns == null) {
                ns = catalog.getDefaultNamespace();
            }
            if (ns != null) {
                ds.getConnectionParameters().put("namespace", ns.getURI());
            }
        }

        //attempt to set the datastore type
        try {
            DataAccessFactory factory = 
                DataStoreUtils.aquireFactory(ds.getConnectionParameters());
            ds.setType(factory.getDisplayName());
        }
        catch(Exception e) {
            LOGGER.warning("Unable to determine datastore type from connection parameters");
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "", e);
            }
        }
        
        catalog.validate((DataStoreInfo)object, false).throwIfInvalid();
        catalog.add( (DataStoreInfo) object );
        
        LOGGER.info( "POST data store " + ds.getName() );
        return ds.getName();
    }

    @Override
    public boolean allowPut() {
        return getAttribute( "datastore" ) != null;
    }
    
    @Override
    protected void handleObjectPut(Object object) throws Exception {
        String workspace = getAttribute("workspace");
        String datastore = getAttribute("datastore");
        
        DataStoreInfo ds = (DataStoreInfo) object;
        DataStoreInfo original = catalog.getDataStoreByName(workspace, datastore);
        
        //ensure this is not a name or workspace change
        if ( ds.getName() != null && !ds.getName().equals( original.getName() ) ) {
            throw new RestletException( "Can't change name of data store.", Status.CLIENT_ERROR_FORBIDDEN );
        }
        if ( ds.getWorkspace() != null && !ds.getWorkspace().equals( original.getWorkspace() ) ) {
            throw new RestletException( "Can't change workspace of data store.", Status.CLIENT_ERROR_FORBIDDEN );
        }
        
        new CatalogBuilder( catalog ).updateDataStore( original, ds );
        
        catalog.validate(original, false).throwIfInvalid();
        catalog.save( original );
        
        clear(original);
        
        LOGGER.info( "PUT data store " + workspace + "," + datastore );
    }
    
    @Override
    public boolean allowDelete() {
        return getAttribute( "datastore" ) != null;
    }
    
    @Override
    protected void handleObjectDelete() throws Exception {
        String workspace = getAttribute("workspace");
        String datastore = getAttribute("datastore");
        boolean recurse = getQueryStringValue("recurse", Boolean.class, false);
        
        DataStoreInfo ds = catalog.getDataStoreByName(workspace, datastore);
        if (!recurse) {
            if ( !catalog.getFeatureTypesByDataStore(ds).isEmpty() ) {
                throw new RestletException( "datastore not empty", Status.CLIENT_ERROR_FORBIDDEN);
            }
            catalog.remove( ds );
        }
        else {
            //recursive delete
            new CascadeDeleteVisitor(catalog).visit(ds);
        }
        clear(ds);
        
        LOGGER.info( "DELETE data store " + workspace + "," + datastore );
    }
    
    void clear(DataStoreInfo info) {
        catalog.getResourcePool().clear(info);
    }
    
    @Override
    protected void configurePersister(XStreamPersister persister, DataFormat format) {
        persister.setCallback( 
            new XStreamPersister.Callback() {
                @Override
                protected Class<DataStoreInfo> getObjectClass() {
                    return DataStoreInfo.class;
                }
                @Override
                protected CatalogInfo getCatalogObject() {
                    String workspace = getAttribute("workspace");
                    String datastore = getAttribute("datastore");
                    
                    if (workspace == null || datastore == null) {
                        return null;
                    }
                    return catalog.getDataStoreByName(workspace, datastore);
                }
                @Override
                protected void postEncodeDataStore(DataStoreInfo ds,
                        HierarchicalStreamWriter writer,
                        MarshallingContext context) {
                    //add a link to the feature types
                    writer.startNode( "featureTypes");
                    encodeCollectionLink("featuretypes", writer);
                    writer.endNode();
                }
                @Override
                protected void postEncodeReference(Object obj, String ref, String prefix,
                        HierarchicalStreamWriter writer, MarshallingContext context) {
                    if ( obj instanceof WorkspaceInfo ) {
                        encodeLink("/workspaces/" + encode(ref), writer );
                    }
                }
            }
        );
    }
    
    static class DataStoreHTMLFormat extends CatalogFreemarkerHTMLFormat {
        Catalog catalog;
        
        public DataStoreHTMLFormat(Request request,
                Response response, Resource resource, Catalog catalog) {
            super(DataStoreInfo.class, request, response, resource);
            this.catalog = catalog;
        }

        @Override
        protected Configuration createConfiguration(Object data, Class clazz) {
            Configuration cfg = 
                super.createConfiguration(data, clazz);
            cfg.setObjectWrapper(new ObjectToMapWrapper<DataStoreInfo>(DataStoreInfo.class) {
                @Override
                protected void wrapInternal(Map properties, SimpleHash model, DataStoreInfo object) {
                    List<FeatureTypeInfo> featureTypes = catalog.getFeatureTypesByDataStore(object);
                    
                    properties.put( "featureTypes", new CollectionModel( featureTypes, new ObjectToMapWrapper(FeatureTypeInfo.class) ) );
                }
            });
            
            return cfg;
        }
    }


}
