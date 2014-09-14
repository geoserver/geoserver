/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.format.ReflectiveJSONFormat;
import org.geoserver.rest.format.ReflectiveXMLFormat;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public abstract class AbstractCatalogListResource extends CatalogResourceBase {

    protected AbstractCatalogListResource(Context context, Request request,
            Response response, Class clazz, Catalog catalog) {
        super(context, request, response, clazz, catalog);
    }

    @Override
    protected final Object handleObjectGet() throws Exception {
        return XStreamPersister.unwrapProxies( handleListGet() );
    }
    
    protected abstract Collection handleListGet() throws Exception;
    
    //JD: we create custom formats here because we need to set up the collection aliases
    // correctly, basically whatever collection we get back we ant to alias to layers, featureTypes,
    // coverages, styles, etc...
    @Override
    protected ReflectiveJSONFormat createJSONFormat(Request request, Response response) {
        final ReflectiveJSONFormat f = super.createJSONFormat(request, response);
        return new ReflectiveJSONFormat() {
            @Override
            public XStream getXStream() {
                return f.getXStream();
            }
            
            @Override
            protected void write(Object data, OutputStream output) throws IOException {
                aliasCollection(data, f.getXStream());
                f.getXStream().toXML(data, output);
            }
        };
    }
    
    @Override
    protected ReflectiveXMLFormat createXMLFormat(Request request, Response response) {
        final ReflectiveXMLFormat f = super.createXMLFormat(request, response);
        return new ReflectiveXMLFormat() {
            @Override
            public XStream getXStream() {
                return f.getXStream();
            }
            
            @Override
            protected void write(Object data, OutputStream output) throws IOException {
                aliasCollection(data, f.getXStream());
                f.getXStream().toXML(data, output);
            }
        };
    }
    
    @Override
    protected void configureXStream(XStream xstream) {
        XStreamPersister xp = xpf.createXMLPersister();
        final String name = getItemName(xp);
        xstream.alias( name, clazz );
        
        xstream.registerConverter( 
            new CollectionConverter(xstream.getMapper()) {
                public boolean canConvert(Class type) {
                    return Collection.class.isAssignableFrom(type);
                };
                @Override
                protected void writeItem(Object item,
                        MarshallingContext context,
                        HierarchicalStreamWriter writer) {
                    
                    writer.startNode( name );
                    context.convertAnother( item );
                    writer.endNode();
                }
            }
        );
        xstream.registerConverter( 
            new Converter() {

                public boolean canConvert(Class type) {
                    return clazz.isAssignableFrom( type );
                }
                
                public void marshal(Object source,
                        HierarchicalStreamWriter writer,
                        MarshallingContext context) {
                    
                    String ref = null;
                    if ( OwsUtils.getter( clazz, "name", String.class ) != null ) {
                        ref = (String) OwsUtils.get( source, "name");
                    }
                    else if ( OwsUtils.getter( clazz, "id", String.class ) != null ) {
                        ref = (String) OwsUtils.get( source, "id");
                    }
                    else {
                        throw new RuntimeException( "Could not determine identifier for: " + clazz.getName());
                    }
                    writer.startNode( "name" );
                    writer.setValue(ref);
                    writer.endNode();
                    
                    encodeLink(encode(ref), writer);
                }

                public Object unmarshal(HierarchicalStreamReader reader,
                        UnmarshallingContext context) {
                    return null;
                }
            }
        );
    }

    protected String getItemName(XStreamPersister xp) {
        return xp.getClassAliasingMapper().serializedClass( clazz );
    }
    
    /**
     * Template method to alias the type of the collection.
     * <p>
     * The default works with list, subclasses may override for instance
     * to work with a Set.
     * </p>
     */
    protected void aliasCollection( Object data, XStream xstream ) {
        XStreamPersister xp = xpf.createXMLPersister();
        final String alias = getItemName(xp);
        xstream.alias(alias + "s", Collection.class, data.getClass());
    }
}
