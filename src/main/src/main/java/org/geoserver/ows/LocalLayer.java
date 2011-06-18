/* Copyright (c) 2001 - 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows;

import org.geoserver.catalog.LayerInfo;

/**
 * A thread local variable for a {@link LayerInfo} that was specified as 
 * part of an ows request.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class LocalLayer {

    /**
     * Key for extended request property
     */
    public static final String KEY = "localLayer";
    
    /**
     * the layer thread local
     */
    static ThreadLocal<LayerInfo> layer = new ThreadLocal<LayerInfo>();
    
    public static void set(LayerInfo l) {
        layer.set(l);
    }
    
    public static LayerInfo get() {
        return layer.get();
    }
    
    public static void remove() {
        layer.remove();
    }
    
}
