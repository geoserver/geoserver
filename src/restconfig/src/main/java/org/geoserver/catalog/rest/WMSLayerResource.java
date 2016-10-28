/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class WMSLayerResource extends AbstractCatalogResource {

    public WMSLayerResource(Context context, Request request,Response response, Catalog catalog) {
        super(context, request, response, WMSLayerInfo.class, catalog);
    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new ResourceHTMLFormat(WMSLayerInfo.class,request,response,this);
    }
    
    @Override
    protected Object handleObjectGet() {
        String workspace = getAttribute( "workspace" );
        String wmsstore = getAttribute( "wmsstore");
        String wmslayer = getAttribute( "wmslayer" );

        if ( wmsstore == null ) {
            LOGGER.fine( "GET feature type" + workspace + "," + wmslayer );
            
            //grab the corresponding namespace for this workspace
            NamespaceInfo ns = catalog.getNamespaceByPrefix( workspace );
            if ( ns != null ) {
                return catalog.getResourceByName(ns,wmslayer, WMSLayerInfo.class);
            }

            throw new RestletException( "", Status.CLIENT_ERROR_NOT_FOUND );
        }

        LOGGER.fine( "GET wms layer " + wmsstore + "," + wmslayer );
        WMSStoreInfo wms = catalog.getStoreByName(workspace, wmsstore, WMSStoreInfo.class);
        return catalog.getResourceByStore( wms, wmslayer, WMSLayerInfo.class );
    }

    @Override
    public boolean allowPost() {
        return getAttribute("wmslayer") == null;
    }
    
    @Override
    protected String handleObjectPost(Object object) throws Exception {
        String workspace = getAttribute( "workspace");
        String wmsstore = getAttribute( "wmsstore");

        WMSLayerInfo wml = (WMSLayerInfo) object;
         
        //ensure the store matches up
        WMSStoreInfo wms = catalog.getStoreByName( workspace, wmsstore, WMSStoreInfo.class);
        if ( wml.getStore() != null ) {
            if ( !wmsstore.equals( wml.getStore().getName() ) ) {
                throw new RestletException( "Expected wms store " + wmsstore +
                " but client specified " + wml.getStore().getName(), Status.CLIENT_ERROR_FORBIDDEN );
            }
        } else {
            wml.setStore( wms );
        }
        
        //ensure workspace/namespace matches up
        if ( wml.getNamespace() != null ) {
            if ( !workspace.equals( wml.getNamespace().getPrefix() ) ) {
                throw new RestletException( "Expected workspace " + workspace +
                    " but client specified " + wml.getNamespace().getPrefix(), Status.CLIENT_ERROR_FORBIDDEN );
            }
        } else {
            wml.setNamespace( catalog.getNamespaceByPrefix( workspace ) );
        }
        wml.setEnabled(true);
        
        NamespaceInfo ns = wml.getNamespace();
        if ( ns != null && !ns.getPrefix().equals( workspace ) ) {
            //TODO: change this once the two can be different and we untie namespace
            // from workspace
            LOGGER.warning( "Namespace: " + ns.getPrefix() + " does not match workspace: " + workspace + ", overriding." );
            ns = null;
        }
        
        if ( ns == null){
            //infer from workspace
            ns = catalog.getNamespaceByPrefix( workspace );
            wml.setNamespace( ns );
        }
        
        // fill in missing information
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setStore(wms);
        cb.initWMSLayer( wml );
        
        wml.setEnabled(true);
        catalog.validate(wml, true).throwIfInvalid();
        catalog.add( wml );
        
        // create a layer for the feature type
        catalog.add(new CatalogBuilder(catalog).buildLayer(wml));
        
        LOGGER.info( "POST wms layer " + wmsstore + "," + wml.getName() );
        return wml.getName();
    }
    
    @Override
    public boolean allowPut() {
        return getAttribute("wmslayer") != null;
    }

    @Override
    protected void handleObjectPut(Object object) throws Exception {
        WMSLayerInfo wml = (WMSLayerInfo) object;
        
        String workspace = getAttribute("workspace");
        String wmsstore = getAttribute("wmsstore");
        String wmslayer = getAttribute("wmslayer");
        
        WMSStoreInfo wms = catalog.getStoreByName(workspace, wmsstore, WMSStoreInfo.class);
        WMSLayerInfo original = catalog.getResourceByStore( wms,  wmslayer, WMSLayerInfo.class );
        new CatalogBuilder(catalog).updateWMSLayer(original,wml);
        catalog.validate(original, false).throwIfInvalid();
        catalog.save( original );
        
        LOGGER.info( "PUT wms layer " + wmsstore + "," + wmslayer );
    }
    
    @Override
    public boolean allowDelete() {
        return getAttribute("wmslayer") != null;
    }
    
    @Override
    public void handleObjectDelete() throws Exception {
        String workspace = getAttribute("workspace");
        String wmsstore = getAttribute("wmsstore");
        String wmslayer = getAttribute("wmslayer");
        boolean recurse = getQueryStringValue("recurse", Boolean.class, false);
        
        WMSStoreInfo wms = catalog.getStoreByName(workspace, wmsstore, WMSStoreInfo.class);
        WMSLayerInfo wml = catalog.getResourceByStore( wms,  wmslayer, WMSLayerInfo.class );
        List<LayerInfo> layers = catalog.getLayers(wml);
        
        if (recurse) {
            //by recurse we clear out all the layers that public this resource
            for (LayerInfo l : layers) {
                catalog.remove(l);
                LOGGER.info( "DELETE layer " + l.getName());
            }
        }
        else {
            if (!layers.isEmpty()) {
                throw new RestletException( "wms layer referenced by layer(s)", Status.CLIENT_ERROR_FORBIDDEN);
            }
        }
        
        catalog.remove( wml);
        
        LOGGER.info( "DELETE wms layer" + wmsstore + "," + wmslayer );
    }

    @Override
    protected void configurePersister(XStreamPersister persister, DataFormat format) {
        persister.setHideFeatureTypeAttributes();
        persister.setCallback( new XStreamPersister.Callback() {
            @Override
            protected Class<WMSLayerInfo> getObjectClass() {
                return WMSLayerInfo.class;
            }
            @Override
            protected CatalogInfo getCatalogObject() {
                String workspace = getAttribute("workspace");
                String wmsstore = getAttribute("wmsstore");
                String wmslayer = getAttribute("wmslayer");
                
                if (workspace == null || wmsstore == null || wmslayer == null) {
                    return null;
                }
                WMSStoreInfo wms = catalog.getStoreByName(workspace, wmsstore, WMSStoreInfo.class);
                if (wms == null) {
                    return null;
                }
                return catalog.getResourceByStore( wms,  wmslayer, WMSLayerInfo.class );
            }
            @Override
            protected void postEncodeReference(Object obj, String ref, String prefix, 
                    HierarchicalStreamWriter writer, MarshallingContext context) {
                if ( obj instanceof NamespaceInfo ) {
                    NamespaceInfo ns = (NamespaceInfo) obj;
                    encodeLink( "/namespaces/" + ns.getPrefix(), writer);
                }
                if ( obj instanceof WMSStoreInfo ) {
                    WMSStoreInfo ds = (WMSStoreInfo) obj;
                    encodeLink( "/workspaces/" + ds.getWorkspace().getName() + "/wmsstores/" + 
                        ds.getName(), writer );
                }
            }
        });
    }
}
