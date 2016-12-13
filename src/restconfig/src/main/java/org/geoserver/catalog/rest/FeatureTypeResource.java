/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeatureType;
import org.restlet.Context;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class FeatureTypeResource extends AbstractCatalogResource {

    public FeatureTypeResource(Context context, Request request,Response response, Catalog catalog) {
        super(context, request, response, FeatureTypeInfo.class, catalog);
    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new ResourceHTMLFormat(FeatureTypeInfo.class,request,response,this);
    }
    
    @Override
    protected Object handleObjectGet() {
        String workspace = getAttribute( "workspace");
        String datastore = getAttribute( "datastore");
        String featureType = getAttribute( "featuretype" );

        FeatureTypeInfo ftInfo;
        
        if (datastore == null) {
            LOGGER.fine( "GET feature type" + workspace + "," + featureType );
            
            //grab the corresponding namespace for this workspace
            NamespaceInfo ns = catalog.getNamespaceByPrefix( workspace );
            if ( ns != null ) {
                ftInfo = catalog.getFeatureTypeByName(ns,featureType);
            } else {
                String message = "No feature found in workspace '" + workspace
                        + "' with name '" + featureType + "'";
                throw new RestletException(message, Status.CLIENT_ERROR_NOT_FOUND);
            }
        } else { // datastore != null
            LOGGER.fine("GET feature type" + datastore + "," + featureType);
            DataStoreInfo dsInfo = catalog.getDataStoreByName(workspace, datastore);
            ftInfo = catalog.getFeatureTypeByDataStore(dsInfo, featureType);
        }
        return ftInfo;
    }

    @Override
    public boolean allowPost() {
        return getAttribute("featuretype") == null;
    }
    
    @Override
    protected String handleObjectPost(Object object) throws Exception {
        String workspace = getAttribute( "workspace");
        String dataStore = getAttribute( "datastore");

        FeatureTypeInfo featureType = (FeatureTypeInfo) object;
         
        //ensure the store matches up
        if ( featureType.getStore() != null ) {
            if ( !dataStore.equals( featureType.getStore().getName() ) ) {
                throw new RestletException( "Expected datastore " + dataStore +
                " but client specified " + featureType.getStore().getName(), Status.CLIENT_ERROR_FORBIDDEN );
            }
        }
        else {
            featureType.setStore( catalog.getDataStoreByName( workspace, dataStore ) );
        }
        
        //ensure workspace/namespace matches up
        if ( featureType.getNamespace() != null ) {
            if ( !workspace.equals( featureType.getNamespace().getPrefix() ) ) {
                throw new RestletException( "Expected workspace " + workspace +
                    " but client specified " + featureType.getNamespace().getPrefix(), Status.CLIENT_ERROR_FORBIDDEN );
            }
        }
        else {
            featureType.setNamespace( catalog.getNamespaceByPrefix( workspace ) );
        }
        featureType.setEnabled(true);
        
        // now, does the feature type exist? If not, create it
        DataStoreInfo ds = catalog.getDataStoreByName( workspace, dataStore );
        DataAccess gtda = ds.getDataStore(null);
        if (gtda instanceof DataStore) {
            String typeName = featureType.getName();
            if(featureType.getNativeName() != null) {
                typeName = featureType.getNativeName(); 
            } 
            boolean typeExists = false;
            DataStore gtds = (DataStore) gtda;
            for(String name : gtds.getTypeNames()) {
                if(name.equals(typeName)) {
                    typeExists = true;
                    break;
                }
            }

            //check to see if this is a virtual JDBC feature type
            MetadataMap mdm = featureType.getMetadata();
            boolean virtual = mdm != null && mdm.containsKey(FeatureTypeInfo.JDBC_VIRTUAL_TABLE);

            if(!virtual && !typeExists) {
                gtds.createSchema(buildFeatureType(featureType));
                // the attributes created might not match up 1-1 with the actual spec due to
                // limitations of the data store, have it re-compute them
                featureType.getAttributes().clear();
                List<String> typeNames = Arrays.asList(gtds.getTypeNames());
                // handle Oracle oddities
                // TODO: use the incoming store capabilites API to better handle the name transformation
                if(!typeNames.contains(typeName) && typeNames.contains(typeName.toUpperCase())) {
                    featureType.setNativeName(featureType.getName().toLowerCase());
                }
            }
        }
        
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.initFeatureType( featureType );

        //attempt to fill in metadata from underlying feature source
        try {
            FeatureSource featureSource = 
                    gtda.getFeatureSource(new NameImpl(featureType.getNativeName()));
            if (featureSource != null) {
                cb.setupMetadata(featureType, featureSource);
            }
        }
        catch(Exception e) {
            LOGGER.log(Level.WARNING, 
                "Unable to fill in metadata from underlying feature source", e);
        }
        
        if ( featureType.getStore() == null ) {
            //get from requests
            featureType.setStore( ds );
        }
        
        NamespaceInfo ns = featureType.getNamespace();
        if ( ns != null && !ns.getPrefix().equals( workspace ) ) {
            //TODO: change this once the two can be different and we untie namespace
            // from workspace
            LOGGER.warning( "Namespace: " + ns.getPrefix() + " does not match workspace: " + workspace + ", overriding." );
            ns = null;
        }
        
        if ( ns == null){
            //infer from workspace
            ns = catalog.getNamespaceByPrefix( workspace );
            featureType.setNamespace( ns );
        }
        
        featureType.setEnabled(true);
        catalog.validate(featureType, true).throwIfInvalid();
        catalog.add( featureType );
        
        //create a layer for the feature type
        catalog.add(new CatalogBuilder(catalog).buildLayer(featureType));
        
        LOGGER.info( "POST feature type" + dataStore + "," + featureType.getName() );
        return featureType.getName();
    }
    
    SimpleFeatureType buildFeatureType(FeatureTypeInfo fti) {
        // basic checks
        if(fti.getName() == null) {
            throw new RestletException("Trying to create new feature type inside the store, " +
            		"but no feature type name was specified", Status.CLIENT_ERROR_BAD_REQUEST);
        } else if(fti.getAttributes() == null || fti.getAttributes() == null) {
            throw new RestletException("Trying to create new feature type inside the store, " +
            		"but no attributes were specified", Status.CLIENT_ERROR_BAD_REQUEST);
        }
        
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        if(fti.getNativeName() != null) {
            builder.setName(fti.getNativeName());
        } else {
            builder.setName(fti.getName());
        }
        if(fti.getNativeCRS() != null) {
            builder.setCRS(fti.getNativeCRS());
        } else if(fti.getCRS() != null) {
            builder.setCRS(fti.getCRS());
        } else if(fti.getSRS() != null) {
            builder.setSRS(fti.getSRS());
        }
        for (AttributeTypeInfo ati : fti.getAttributes()) {
            if(ati.getLength() != null && ati.getLength() > 0) {
                builder.length(ati.getLength());
            }
            builder.nillable(ati.isNillable());
            builder.add(ati.getName(), ati.getBinding());
        }
        return builder.buildFeatureType();
    }

    @Override
    public boolean allowPut() {
        return getAttribute("featuretype") != null;
    }

    @Override
    protected void handleObjectPut(Object object) throws Exception {
        FeatureTypeInfo featureTypeUpdate = (FeatureTypeInfo) object;
        
        String workspace = getAttribute("workspace");
        String datastore = getAttribute("datastore");
        String featuretype = getAttribute("featuretype");
        
        DataStoreInfo ds = catalog.getDataStoreByName(workspace, datastore);
        FeatureTypeInfo featureTypeInfo = catalog.getFeatureTypeByDataStore( ds,  featuretype );
        Map<String, Serializable> parametersCheck = featureTypeInfo.getStore().getConnectionParameters();
        
        calculateOptionalFields(featureTypeUpdate, featureTypeInfo);
        CatalogBuilder helper = new CatalogBuilder(catalog);
        helper.updateFeatureType(featureTypeInfo,featureTypeUpdate);
        
        catalog.validate(featureTypeInfo, false).throwIfInvalid();
        catalog.save( featureTypeInfo );
        catalog.getResourcePool().clear(featureTypeInfo);
        
        Map<String, Serializable> parameters = featureTypeInfo.getStore().getConnectionParameters();
        MetadataMap mdm = featureTypeInfo.getMetadata();
        boolean virtual = mdm != null && mdm.containsKey(FeatureTypeInfo.JDBC_VIRTUAL_TABLE);
        
        if( !virtual && parameters.equals(parametersCheck)){
            LOGGER.info( "PUT FeatureType" + datastore + "," + featuretype + " updated metadata only");
        }
        else {
            LOGGER.info( "PUT featureType" + datastore + "," + featuretype + " updated metadata and data access" );
            catalog.getResourcePool().clear(featureTypeInfo.getStore());
        }
    }
    
    @Override
    public boolean allowDelete() {
        return getAttribute("featuretype") != null;
    }
    
    @Override
    public void handleObjectDelete() throws Exception {
        String workspace = getAttribute("workspace");
        String datastore = getAttribute("datastore");
        String featuretype = getAttribute("featuretype");
        boolean recurse = getQueryStringValue("recurse", Boolean.class, false);
        
        DataStoreInfo ds = catalog.getDataStoreByName(workspace, datastore);
        FeatureTypeInfo ft = catalog.getFeatureTypeByDataStore( ds,  featuretype );
        List<LayerInfo> layers = catalog.getLayers(ft);
            
        if (recurse) {
            //by recurse we clear out all the layers that public this resource
            for (LayerInfo l : layers) {
                catalog.remove(l);
                LOGGER.info( "DELETE layer " + l.getName());
            }
        }
        else {
            if (!layers.isEmpty()) {
                throw new RestletException( "feature type referenced by layer(s)", Status.CLIENT_ERROR_FORBIDDEN);
            }
        }
        
        catalog.remove( ft );
        
        LOGGER.info( "DELETE feature type" + datastore + "," + featuretype );
    }

    @Override
    protected void configurePersister(XStreamPersister persister, DataFormat format) {
        if(getRequest().getMethod() == Method.GET) {
            persister.setHideFeatureTypeAttributes();
        }
        persister.setCallback( new XStreamPersister.Callback() {
            @Override
            protected Class<FeatureTypeInfo> getObjectClass() {
                return FeatureTypeInfo.class;
            }
            @Override
            protected CatalogInfo getCatalogObject() {
                String workspace = getAttribute("workspace");
                String datastore = getAttribute("datastore");
                String featuretype = getAttribute("featuretype");
                
                if (workspace == null || datastore == null || featuretype == null) {
                    return null;
                }
                DataStoreInfo ds = catalog.getDataStoreByName(workspace, datastore);
                if (ds == null) {
                    return null;
                }
                return catalog.getFeatureTypeByDataStore( ds,  featuretype );
            }
            @Override
            protected void postEncodeReference(Object obj, String ref, String prefix,
                    HierarchicalStreamWriter writer, MarshallingContext context) {
                if ( obj instanceof NamespaceInfo ) {
                    NamespaceInfo ns = (NamespaceInfo) obj;
                    encodeLink( "/namespaces/" + encode(ns.getPrefix()), writer);
                }
                if ( obj instanceof DataStoreInfo ) {
                    DataStoreInfo ds = (DataStoreInfo) obj;
                    encodeLink( "/workspaces/" + encode(ds.getWorkspace().getName()) + 
                        "/datastores/" + encode(ds.getName()), writer );
                }
            }
            
            @Override
            protected void postEncodeFeatureType(FeatureTypeInfo ft,
                    HierarchicalStreamWriter writer, MarshallingContext context) {
                try {
                    writer.startNode("attributes");
                    context.convertAnother(ft.attributes());
                    writer.endNode();
                } catch (IOException e) {
                    throw new RuntimeException("Could not get native attributes", e);
                }
            }
        });
    }
}
