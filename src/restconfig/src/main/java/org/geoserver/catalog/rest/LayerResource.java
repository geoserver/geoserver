/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.format.DataFormat;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class LayerResource extends AbstractCatalogResource {

    public LayerResource(Context context, Request request, Response response,
         Catalog catalog) {
        super(context, request, response, LayerInfo.class, catalog);
    }
    
    @Override
    protected Object handleObjectGet() throws Exception {
        String layer = getAttribute( "layer" );
        
        if ( layer == null ) {
            //return all layers
            return catalog.getLayers();
        }
        
        return catalog.getLayerByName( layer ); 
        
    }

    @Override
    protected String handleObjectPost(Object object) throws Exception {
        return null;
    }

    @Override
    public boolean allowPut() {
        return getAttribute( "layer" ) != null;
    }
    
    @Override
    protected void handleObjectPut(Object object) throws Exception {
        String l = getAttribute( "layer" );
        LayerInfo original = catalog.getLayerByName(l);
        LayerInfo layer = (LayerInfo) object;
        
        //ensure this is not a name change
        // TODO: Uncomment this when the resource/layer split is not, now by definition 
        // we cannot rename a layer, it's just not possible and it's not un-marshalled either
//        if ( layer.getName() != null && !layer.getName().equals( original.getName() ) ) {
//            throw new RestletException( "Can't change name of a layer", Status.CLIENT_ERROR_FORBIDDEN );
//        }
        // force in the same resource otherwise the update will simply fail as we cannot reach the name
        layer.setResource(original.getResource());
        
        new CatalogBuilder( catalog ).updateLayer( original, layer );
        catalog.save( original );

        LOGGER.info( "PUT layer " + l);
    }
    
    @Override
    public boolean allowDelete() {
        return getAttribute("layer") != null;
    }
    
    @Override
    protected void handleObjectDelete() throws Exception {
        String l = getAttribute("layer");
        boolean recurse = getQueryStringValue("recurse", Boolean.class, false);
        
        LayerInfo layer = (LayerInfo) catalog.getLayerByName(l);
        if (!recurse) {
            catalog.remove(layer);
        }
        else {
            new CascadeDeleteVisitor(catalog).visit(layer);
        }

        LOGGER.info( "DELETE layer " + l);
    }
    
    @Override
    protected void configurePersister(XStreamPersister persister, DataFormat format) {
        persister.setCallback(new XStreamPersister.Callback() {
            @Override
            protected Class<LayerInfo> getObjectClass() {
                return LayerInfo.class;
            }
            @Override
            protected CatalogInfo getCatalogObject() {
                String l = getAttribute( "layer" );
                if (l == null) {
                    return null;
                }
                return catalog.getLayerByName(l);
            }
            @Override
            protected void postEncodeReference(Object obj, String ref, String prefix, 
                    HierarchicalStreamWriter writer, MarshallingContext context) {
                if ( obj instanceof StyleInfo ) {
                    StyleInfo style = (StyleInfo) obj;
                    StringBuffer link = new StringBuffer();
                    if (style.getWorkspace() != null) {
                        String wsName = style.getWorkspace().getName();
                        writer.startNode("workspace");
                        writer.setValue(wsName);
                        writer.endNode();
                        link.append("/workspaces/").append(encode(wsName));
                    }
                    link.append("/styles/").append(encode(style.getName()));
                    encodeLink(link.toString(), writer);
                }
                if ( obj instanceof ResourceInfo ) {
                    ResourceInfo r = (ResourceInfo) obj;
                    StringBuffer link = new StringBuffer( "/workspaces/" )
                        .append( encode(r.getStore().getWorkspace().getName()) ).append( "/" );

                    if ( r instanceof FeatureTypeInfo ) {
                        link.append( "datastores/").append( encode(r.getStore().getName()) )
                            .append( "/featuretypes/");
                    }
                    else if ( r instanceof CoverageInfo ) {
                        link.append( "coveragestores/").append( encode(r.getStore().getName()) )
                            .append( "/coverages/");
                    }
                    else if ( r instanceof WMSLayerInfo ) {
                        link.append( "wmsstores/").append( encode(r.getStore().getName()) )
                            .append( "/wmslayers/");
                    }
                    else {
                        return;
                    }
                    
                    link.append( encode(r.getName()) );
                    encodeLink(link.toString(), writer);
                }
            }
        });
        
    }
    
}
