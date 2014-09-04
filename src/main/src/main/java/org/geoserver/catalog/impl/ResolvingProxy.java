/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;


/**
 * A proxy which holds onto an identifier which will later be 
 * resolved into a real object.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class ResolvingProxy extends ProxyBase {

    /**
     * Wraps an object in the proxy.
     * 
     * @throws RuntimeException If creating the proxy fails.
     */
    public static <T> T create( String ref, Class<T> clazz ) {
        return create(ref, null, clazz);
    }

    /**
     * Wraps an object in the proxy, specifying a prefix for the reference.
     * 
     * @throws RuntimeException If creating the proxy fails.
     */
    public static <T> T create( String ref, String prefix, Class<T> clazz ) {
        InvocationHandler h = new ResolvingProxy(ref, prefix);
        
        Class proxyClass = 
            Proxy.getProxyClass( clazz.getClassLoader(), clazz );
        
        T proxy;
        try {
            proxy = (T) proxyClass.getConstructor(
                new Class[] { InvocationHandler.class }).newInstance(new Object[] { h } );
        }
        catch( Exception e ) {
            throw new RuntimeException( e );
        }
        
        return proxy;
    }
    
    public static <T> T resolve( Catalog catalog, T object ) {
        if ( object instanceof Proxy ) {
            InvocationHandler h = Proxy.getInvocationHandler( object );
            if ( h instanceof ResolvingProxy ) {
                String ref = ((ResolvingProxy)h).getRef();
                String pre = ((ResolvingProxy)h).getPrefix();

                if ( object instanceof WorkspaceInfo ) {
                    Object ws = catalog.getWorkspace( ref );
                    if ( ws == null ) { 
                        ws = catalog.getWorkspaceByName( ref );
                    }
                    return (T) ws;
                }
                if ( object instanceof NamespaceInfo ) {
                    
                     Object ns = catalog.getNamespace( ref );
                     if ( ns == null ) {
                         ns = catalog.getNamespaceByPrefix( ref );
                     }
                     return (T) ns;
                }
                if ( object instanceof StoreInfo ) {
                    if ( object instanceof DataStoreInfo ) {
                        return (T) catalog.getDataStore( ref );
                    }
                    if ( object instanceof CoverageStoreInfo ) {
                        return (T) catalog.getCoverageStore( ref );
                    }
                    
                    return (T) catalog.getStore( ref, StoreInfo.class );
                }
                if ( object instanceof ResourceInfo ) {
                    if ( object instanceof FeatureTypeInfo ) {
                        return (T) catalog.getFeatureType( ref );
                    }
                    if ( object instanceof CoverageInfo ) {
                        return (T) catalog.getCoverage( ref );
                    }
                    
                    return (T) catalog.getResource( ref, ResourceInfo.class );
                }
                if ( object instanceof LayerInfo ) {
                    Object l = catalog.getLayer( ref );
                    if ( l == null ) {
                        l = catalog.getLayerByName( ref );
                    }
                    return (T) l; 
                }
                if ( object instanceof LayerGroupInfo ) {
                    Object g = catalog.getLayerGroup( ref );
                    if ( g == null ) {
                        g = catalog.getLayerGroupByName( ref );
                    }
                    return (T) g; 
                }                
                if ( object instanceof StyleInfo ) {
                    Object s = catalog.getStyle( ref );
                    if ( s == null ) {
                        if (pre != null) {
                            //look up in workspace
                            s = catalog.getStyleByName(pre, ref);
                        }
                    }
                    if (s == null) {
                        //still no luck
                        s = catalog.getStyleByName( ref );
                    }
                    return (T) s;
                }
            }
        }
        
        return object;
    }
    
    /**
     * the reference
     */
    String ref;
    
    /**
     * optional prefix, used to reference by name inside of a workspace
     */
    String prefix;
    
    public ResolvingProxy(String ref) {
        this(ref, null);
    }
    
    public ResolvingProxy(String ref, String prefix) {
        this.ref = ref;
        this.prefix = prefix;
    }

    public String getRef() {
        return ref;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    protected Object handleGetUnSet(Object proxy, Method method, String property) throws Throwable {
        if ( "id".equalsIgnoreCase( property ) ) {
            return ref;
        }
            
        return null;
    }
    
    @Override
    protected Object handleOther(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }
}
